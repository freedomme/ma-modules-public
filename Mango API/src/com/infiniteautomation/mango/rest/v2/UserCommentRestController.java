/**
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 * @author Terry Packer
 */
package com.infiniteautomation.mango.rest.v2;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.servlet.http.HttpServletRequest;

import org.jooq.Field;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.infiniteautomation.mango.rest.v2.model.StreamedArrayWithTotal;
import com.infiniteautomation.mango.rest.v2.model.StreamedBasicVORqlQueryWithTotal;
import com.infiniteautomation.mango.rest.v2.model.comment.UserCommentModel;
import com.infiniteautomation.mango.spring.db.UserCommentTableDefinition;
import com.infiniteautomation.mango.spring.db.UserTableDefinition;
import com.infiniteautomation.mango.spring.service.UserCommentService;
import com.infiniteautomation.mango.util.RQLUtils;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.comment.UserCommentVO;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import net.jazdw.rql.parser.ASTNode;

/**
 *
 *
 * @author Terry Packer
 *
 */
@Api(value="User Comments")
@RestController()
@RequestMapping("/comments")
public class UserCommentRestController {

    private final UserCommentService service;
    private final Map<String, Function<Object, Object>> valueConverterMap;
    private final Map<String, Field<?>> fieldMap;
    private final BiFunction<UserCommentVO, PermissionHolder, UserCommentModel> map = (vo, user) -> {return new UserCommentModel(vo);};

    @Autowired
    public UserCommentRestController(UserCommentService service, UserCommentTableDefinition userCommentTable, UserTableDefinition userTable){
        this.service = service;
        this.valueConverterMap = new HashMap<>();
        this.valueConverterMap.put("valueConverterMap", (toConvert) -> {
            return UserCommentVO.COMMENT_TYPE_CODES.getId((String)toConvert);
        });
        this.fieldMap = new HashMap<>();
        this.fieldMap.put("username", userTable.getAlias("username"));
        this.fieldMap.put("referenceId", userCommentTable.getAlias("typeKey"));
        this.fieldMap.put("timestamp", userCommentTable.getAlias("ts"));
    }

    @ApiOperation(
            value = "Get all User Comments",
            notes = "",
            response=UserCommentModel.class,
            responseContainer="Array"
            )
    @RequestMapping(method = RequestMethod.GET, value="/list")
    public StreamedArrayWithTotal getAll(HttpServletRequest request,
            @RequestParam(value="limit", required=false, defaultValue="100")Integer limit,
            @AuthenticationPrincipal User user) {

        return doQuery(new ASTNode("limit", limit), user);
    }

    @ApiOperation(
            value = "Query User Comments",
            notes = "",
            response=UserCommentModel.class,
            responseContainer="Array"
            )
    @RequestMapping(method = RequestMethod.POST, value = "/query")
    public StreamedArrayWithTotal query(

            @ApiParam(value="Query", required=true)
            @RequestBody(required=true) ASTNode query,
            @AuthenticationPrincipal User user) {
        return doQuery(query, user);
    }

    @ApiOperation(
            value = "Query User Comments",
            notes = "",
            response=UserCommentModel.class,
            responseContainer="Array"
            )
    @RequestMapping(method = RequestMethod.GET)
    public StreamedArrayWithTotal queryRQL(
            @AuthenticationPrincipal User user,
            HttpServletRequest request) {
        ASTNode query = RQLUtils.parseRQLtoAST(request.getQueryString());
        return doQuery(query, user);
    }

    /**
     * Create a new User Comment
     *
     * The timestamp and UserID are optional
     * Username is not used for input
     *
     * @param model
     * @param request
     * @return
     * @throws RestValidationFailedException
     */
    @ApiOperation(
            value = "Create New User Comment",
            notes = ""
            )
    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<UserCommentModel> createNewUserComment(
            @ApiParam( value = "User Comment to save", required = true )
            @RequestBody(required=true)
            UserCommentModel model,
            @AuthenticationPrincipal User user,
            UriComponentsBuilder builder) {

        if(model.getTimestamp() <= 0) {
            model.setTimestamp(System.currentTimeMillis());
        }

        //Assign a userId if there isn't one
        if(model.getUserId() <= 0){
            model.setUserId(user.getId());
            model.setUsername(user.getUsername());
        }
        UserCommentVO vo = service.insert(model.toVO());
        URI location = builder.path("/comments/{xid}").buildAndExpand(vo.getXid()).toUri();
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(location);
        return new ResponseEntity<>(map.apply(vo, user), headers, HttpStatus.OK);
    }

    @ApiOperation(value = "Delete A User Comment by XID")
    @RequestMapping(method = RequestMethod.DELETE, value = "/{xid}")
    public UserCommentModel deleteUserComment(
            @ApiParam(value = "xid", required = true, allowMultiple = false)
            @PathVariable String xid,
            @AuthenticationPrincipal User user) {
        return map.apply(service.delete(xid), user);
    }

    @ApiOperation(value = "Get user comment by xid", notes = "Returns the user comment specified by the given xid")
    @RequestMapping(method = RequestMethod.GET, value = "/{xid}")
    public UserCommentModel getUserComment(
            @ApiParam(value = "Valid xid", required = true, allowMultiple = false)
            @PathVariable String xid, @AuthenticationPrincipal User user) {
        return map.apply(service.get(xid), user);
    }

    @ApiOperation(value = "Updates a user comment")
    @RequestMapping(method = RequestMethod.PUT, value = "/{xid}")
    public ResponseEntity<UserCommentModel> updateUserComment(
            @PathVariable String xid,
            @RequestBody(required=true) UserCommentModel model,
            @AuthenticationPrincipal User user,
            UriComponentsBuilder builder) {
        //Change the owner
        if(model.getUserId() == 0){
            model.setUserId(user.getId());
            model.setUsername(user.getUsername());
        }
        UserCommentVO updated = service.update(xid, model.toVO());
        URI location = builder.path("/users/{username}").buildAndExpand(updated.getUsername()).toUri();
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(location);
        return new ResponseEntity<>(map.apply(updated, user), headers, HttpStatus.OK);
    }

    protected StreamedArrayWithTotal doQuery(ASTNode rql, User user) {
        return new StreamedBasicVORqlQueryWithTotal<>(service, rql, fieldMap, valueConverterMap, vo -> service.hasReadPermission(user, vo), vo -> map.apply(vo, user));
    }
}