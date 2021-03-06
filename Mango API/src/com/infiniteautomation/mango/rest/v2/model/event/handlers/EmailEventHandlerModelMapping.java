/**
 * Copyright (C) 2018  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.rest.v2.model.event.handlers;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.infiniteautomation.mango.rest.v2.model.RestModelJacksonMapping;
import com.infiniteautomation.mango.rest.v2.model.RestModelMapper;
import com.infiniteautomation.mango.rest.v2.model.mailingList.EmailRecipientModel;
import com.serotonin.m2m2.vo.event.EmailEventHandlerVO;
import com.serotonin.m2m2.vo.mailingList.MailingListRecipient;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 * @author Terry Packer
 *
 */
@Component
public class EmailEventHandlerModelMapping implements RestModelJacksonMapping<EmailEventHandlerVO, EmailEventHandlerModel> {

    @Override
    public EmailEventHandlerModel map(Object o, PermissionHolder user, RestModelMapper mapper) {
        EmailEventHandlerVO vo = (EmailEventHandlerVO)o;
        EmailEventHandlerModel model = new EmailEventHandlerModel(vo);

        if(vo.getActiveRecipients() != null) {
            List<EmailRecipientModel> activeRecipients = new ArrayList<>();
            model.setActiveRecipients(activeRecipients);

            for(MailingListRecipient bean : vo.getActiveRecipients()) {
                activeRecipients.add(mapper.map(bean, EmailRecipientModel.class, user));
            }
        }

        if(vo.getEscalationRecipients() != null) {
            List<EmailRecipientModel> escalationRecipients = new ArrayList<>();
            model.setEscalationRecipients(escalationRecipients);

            for(MailingListRecipient bean : vo.getEscalationRecipients()) {
                escalationRecipients.add(mapper.map(bean, EmailRecipientModel.class, user));
            }
        }

        if(vo.getInactiveRecipients() != null) {
            List<EmailRecipientModel> inactiveRecipients = new ArrayList<>();
            model.setInactiveRecipients(inactiveRecipients);

            for(MailingListRecipient bean : vo.getInactiveRecipients()) {
                inactiveRecipients.add(mapper.map(bean, EmailRecipientModel.class, user));
            }
        }

        return model;
    }

    @Override
    public Class<EmailEventHandlerModel> toClass() {
        return EmailEventHandlerModel.class;
    }

    @Override
    public Class<EmailEventHandlerVO> fromClass() {
        return EmailEventHandlerVO.class;
    }

    @Override
    public String getTypeName() {
        return "EMAIL";
    }

}
