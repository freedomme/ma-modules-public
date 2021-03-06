/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.infiniteautomation.mango.rest.v2.exception;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.io.EofException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.NestedRuntimeException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.infiniteautomation.mango.db.query.RQLToCondition.RQLVisitException;
import com.infiniteautomation.mango.io.messaging.MessageSendException;
import com.infiniteautomation.mango.io.messaging.email.EmailFailedException;
import com.infiniteautomation.mango.rest.v2.advice.MangoRequestBodyAdvice;
import com.infiniteautomation.mango.rest.v2.model.RestModelMapper;
import com.infiniteautomation.mango.rest.v2.views.AdminView;
import com.infiniteautomation.mango.spring.components.EmailAddressVerificationService.EmailAddressInUseException;
import com.infiniteautomation.mango.spring.script.MangoScriptException;
import com.infiniteautomation.mango.spring.service.PermissionService;
import com.infiniteautomation.mango.util.exception.FeatureDisabledException;
import com.infiniteautomation.mango.util.exception.InvalidRQLException;
import com.infiniteautomation.mango.util.exception.NotFoundException;
import com.infiniteautomation.mango.util.exception.TranslatableExceptionI;
import com.infiniteautomation.mango.util.exception.TranslatableIllegalStateException;
import com.infiniteautomation.mango.util.exception.TranslatableRuntimeException;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.i18n.ProcessResult;
import com.serotonin.m2m2.i18n.TranslatableException;
import com.serotonin.m2m2.module.DefaultPagesDefinition;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.permission.PermissionException;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.web.mvc.spring.security.authentication.MangoPasswordAuthenticationProvider.AuthenticationRateException;

/**
 *
 * @author Terry Packer
 */
@ControllerAdvice(basePackages= {"com.infiniteautomation.mango.rest.v2"})
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

    private final Log LOG = LogFactory.getLog(RestExceptionHandler.class);

    final RequestMatcher browserHtmlRequestMatcher;
    final RestModelMapper mapper;
    final PermissionService service;

    @Autowired
    public RestExceptionHandler(
            @Qualifier("browserHtmlRequestMatcher")
            RequestMatcher browserHtmlRequestMatcher,
            RestModelMapper mapper,
            PermissionService service) {
        this.browserHtmlRequestMatcher = browserHtmlRequestMatcher;
        this.mapper = mapper;
        this.service = service;
    }

    /**
     * Handle End of file exceptions specifically those from the client's connection being closed
     * @param request
     * @param response
     * @param ex
     * @param req
     * @return
     */
    @ExceptionHandler(EofException.class)
    public Object exceptionHandler(HttpServletRequest request, HttpServletResponse response, IOException ex, WebRequest req) {
        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        this.storeException(request, ex, HttpStatus.SERVICE_UNAVAILABLE);
        //There is nothing we can send back
        return null;
    }

    //Anything that extends our Base Exception
    @ExceptionHandler({AbstractRestV2Exception.class})
    public ResponseEntity<Object> handleMangoError(HttpServletRequest request, HttpServletResponse response, AbstractRestV2Exception ex, WebRequest req) {
        return handleExceptionInternal(ex, ex, new HttpHeaders(), ex.getStatus(), req);
    }

    @ExceptionHandler({
        org.springframework.security.access.AccessDeniedException.class,
        PermissionException.class
    })
    public ResponseEntity<Object> handleAccessDenied(HttpServletRequest request, HttpServletResponse response, Exception ex, WebRequest req){
        Object model;

        if (ex instanceof PermissionException) {
            PermissionException permissionException = (PermissionException) ex;
            model = new AccessDeniedException(permissionException.getTranslatableMessage(), ex);
        } else {
            model = new AccessDeniedException(ex);
        }

        return handleExceptionInternal(ex, model, new HttpHeaders(), HttpStatus.FORBIDDEN, req);
    }

    @ExceptionHandler({
        ValidationException.class
    })
    public ResponseEntity<Object> handleValidationException(HttpServletRequest request, HttpServletResponse response, Exception ex, WebRequest req) {
        ValidationException validationException = (ValidationException) ex;

        ProcessResult result = validationException.getValidationResult();

        //Do any potential mapping of VO property names to model property names
        Class<?> modelClass = (Class<?>) req.getAttribute(MangoRequestBodyAdvice.MODEL_CLASS, RequestAttributes.SCOPE_REQUEST);
        if(validationException.getValidatedClass() != null && modelClass != null) {
            result = mapper.mapValidationErrors(modelClass, validationException.getValidatedClass(), result);
        }

        return handleExceptionInternal(ex, new ValidationFailedRestException(result), new HttpHeaders(), HttpStatus.UNPROCESSABLE_ENTITY, req);
    }

    @ExceptionHandler({
        MessageSendException.class
    })
    public ResponseEntity<Object> handleEmailFailedException(HttpServletRequest request, HttpServletResponse response, Exception ex, WebRequest req) {
        if(ex instanceof EmailFailedException) {
            EmailFailedException e = (EmailFailedException)ex;
            return handleExceptionInternal(ex, new SendEmailFailedRestException(e, e.getSession()), new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, req);
        }else {
            return handleExceptionInternal(ex, new SendMessageFailedRestException((MessageSendException)ex), new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, req);
        }
    }

    @ExceptionHandler({
        NotFoundException.class,
        ResourceNotFoundException.class
    })
    public ResponseEntity<Object> handleNotFoundException(HttpServletRequest request, HttpServletResponse response, Exception ex, WebRequest req) {
        return handleExceptionInternal(ex, new NotFoundRestException(ex), new HttpHeaders(), HttpStatus.NOT_FOUND, req);
    }

    @ExceptionHandler({
        InvalidRQLException.class
    })
    public ResponseEntity<Object> handleInvalidRQLException(HttpServletRequest request, HttpServletResponse response, InvalidRQLException ex, WebRequest req) {
        return handleExceptionInternal(ex, new InvalidRQLRestException(ex), new HttpHeaders(), HttpStatus.BAD_REQUEST, req);
    }

    @ExceptionHandler({
        RQLVisitException.class
    })
    public ResponseEntity<Object> handleRQLVisitException(HttpServletRequest request, HttpServletResponse response, RQLVisitException ex, WebRequest req) {
        return handleExceptionInternal(ex, new RQLVisitRestException(ex), new HttpHeaders(), HttpStatus.BAD_REQUEST, req);
    }

    @ExceptionHandler({
        TranslatableIllegalStateException.class
    })
    public ResponseEntity<Object> handleTranslatableIllegalStateException(HttpServletRequest request, HttpServletResponse response, TranslatableIllegalStateException ex, WebRequest req) {
        return handleExceptionInternal(ex, new IllegalStateRestException(ex.getTranslatableMessage(), ex), new HttpHeaders(), HttpStatus.BAD_REQUEST, req);
    }

    @ExceptionHandler({
        EmailAddressInUseException.class,
        FeatureDisabledException.class
    })
    public ResponseEntity<Object> handleConflictExceptions(HttpServletRequest request, HttpServletResponse response, Exception ex, WebRequest req) {
        ConfictRestException body;
        if (ex instanceof FeatureDisabledException) {
            body = new ConfictRestException(MangoRestErrorCode.FEATURE_DISABLED, ((FeatureDisabledException) ex).getTranslatableMessage(), ex);
        } else if (ex instanceof TranslatableExceptionI) {
            body = new ConfictRestException(((TranslatableExceptionI) ex).getTranslatableMessage(), ex);
        } else {
            body = new ConfictRestException(ex);
        }
        return handleExceptionInternal(ex, body, new HttpHeaders(), body.getStatus(), req);
    }

    @ExceptionHandler({
        TranslatableRuntimeException.class
    })
    public ResponseEntity<Object> handleTranslatableRuntimeException(HttpServletRequest request, HttpServletResponse response, TranslatableRuntimeException ex, WebRequest req) {

        GenericRestException body = new GenericRestException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getTranslatableMessage(), ex.getCause());
        return handleExceptionInternal(ex, body, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, req);
    }

    @ExceptionHandler({
        TranslatableException.class
    })
    public ResponseEntity<Object> handleTranslatableException(HttpServletRequest request, HttpServletResponse response, TranslatableException ex, WebRequest req) {

        GenericRestException body = new GenericRestException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getTranslatableMessage(), ex.getCause());
        return handleExceptionInternal(ex, body, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, req);
    }

    @ExceptionHandler(AuthenticationRateException.class)
    public ResponseEntity<Object> handleIpAddressAuthenticationRateException(HttpServletRequest request, HttpServletResponse response, AuthenticationRateException ex, WebRequest req) {
        RateLimitedRestException body = RateLimitedRestException.restExceptionFor(ex);
        return handleExceptionInternal(ex, body, new HttpHeaders(), body.getStatus(), req);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Object> handleUsernameAuthenticationRateException(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex, WebRequest req) {
        AuthenticationFailedRestException body = AuthenticationFailedRestException.restExceptionFor(ex);
        return handleExceptionInternal(ex, body, new HttpHeaders(), body.getStatus(), req);
    }

    @ExceptionHandler(MangoScriptException.class)
    public ResponseEntity<Object> handleMangoScriptException(HttpServletRequest request, HttpServletResponse response, MangoScriptException ex, WebRequest req) {
        ScriptRestException body = new ScriptRestException(ex);
        return handleExceptionInternal(ex, body, new HttpHeaders(), body.getStatus(), req);
    }

    @ExceptionHandler({
        Exception.class
    })
    public ResponseEntity<Object> handleAllOtherErrors(HttpServletRequest request, HttpServletResponse response, Exception ex, WebRequest req){
        return handleExceptionInternal(ex, new ServerErrorException(ex), new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, req);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex,
            Object body, HttpHeaders headers, HttpStatus status, WebRequest request) {

        if (status == HttpStatus.UNAUTHORIZED || status == HttpStatus.FORBIDDEN || status == HttpStatus.TOO_MANY_REQUESTS) {
            if (LOG.isWarnEnabled()) {
                LOG.warn(String.format("REST request status %s: %s", status, request.getDescription(true)), ex);
            }
        } else if (status.value() >= 500) {
            if (LOG.isErrorEnabled()) {
                LOG.error(String.format("REST request status %s: %s", status, request.getDescription(true)), ex);
            }
        } else if (status.value() >= 400) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("REST request status %s: %s", status, request.getDescription(true)), ex);
            }
        }

        HttpServletRequest servletRequest = ((ServletWebRequest) request).getRequest();
        HttpServletResponse servletResponse = ((ServletWebRequest) request).getResponse();

        this.storeException(servletRequest, ex, status);

        if (this.browserHtmlRequestMatcher.matches(servletRequest) && !Common.envProps.getBoolean("rest.disableErrorRedirects", false)) {
            String uri;
            if (status == HttpStatus.FORBIDDEN) {
                // browser HTML request
                PermissionHolder holder;
                User user;
                try {
                    holder = Common.getUser();
                }catch(PermissionException e) {
                    holder = null;
                }
                if(!(holder instanceof User)) {
                    user = null;
                }else {
                    user = (User)holder;
                }
                uri = DefaultPagesDefinition.getUnauthorizedUri(servletRequest, servletResponse, user);

                // Put exception into request scope (perhaps of use to a view)
                servletRequest.setAttribute(WebAttributes.ACCESS_DENIED_403, ex);

                // Set the 403 status code.
                servletResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
            } else if(status == HttpStatus.NOT_FOUND) {
                uri = DefaultPagesDefinition.getNotFoundUri(servletRequest, servletResponse);
            } else {
                uri = DefaultPagesDefinition.getErrorUri(servletRequest, servletResponse);
            }
            try {
                servletResponse.sendRedirect(uri);
            } catch (IOException e) {
                LOG.error(e.getMessage(), e);
            }
            return null;
        } else {
            // To strip off the double messages generated by this...
            if (ex instanceof NestedRuntimeException)
                ex = (Exception) ((NestedRuntimeException) ex).getMostSpecificCause();

            // If no body provided we will create one
            if (body == null)
                body = new GenericRestException(status, ex);

            //Add admin view if necessary
            PermissionHolder user;
            try{
                user = Common.getUser();
            }catch(PermissionException e) {
                user = null;
            }
            MappingJacksonValue value = new MappingJacksonValue(body);
            if(user != null && service.hasAdminRole(user))
                value.setSerializationView(AdminView.class);
            else
                value.setSerializationView(Object.class);
            body = value;
            return new ResponseEntity<Object>(body, headers, status);
        }
    }

    /**
     * Store the exception into the session if one exists
     *
     * @param request
     * @param ex
     */
    protected void storeException(HttpServletRequest request, Exception ex, HttpStatus status){
        // Set Exception into Context
        HttpSession sesh = request.getSession(false);
        if (sesh != null)
            sesh.setAttribute(Common.SESSION_USER_EXCEPTION, ex);
    }

}
