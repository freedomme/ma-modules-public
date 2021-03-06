/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.infiniteautomation.mango.rest.v2;

import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.infiniteautomation.mango.db.query.pojo.RQLFilterJavaBean;
import com.infiniteautomation.mango.rest.v2.model.FilteredStreamWithTotal;
import com.infiniteautomation.mango.rest.v2.model.RestModelMapper;
import com.infiniteautomation.mango.rest.v2.model.StreamWithTotal;
import com.infiniteautomation.mango.rest.v2.model.event.EventInstanceModel;
import com.infiniteautomation.mango.util.RQLUtils;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.rt.event.EventInstance;
import com.serotonin.m2m2.vo.User;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import net.jazdw.rql.parser.ASTNode;

/**
 * REST Endpoints for User Event Cache access, All un-acknowledged user events are held in the cache
 * on a per user basis.
 *
 * Note that querying an object is different to querying the database.  If a property is queried on
 * that does not exist in a given object, it will not fail and simply not match that criteria.  Since
 * the list of user events contains various event types, each item in the list can have different properties.
 *
 * @author Terry Packer
 */
@Api(value="User Events", description="User events are all un-acknowledged events for a user")
@RestController()
@RequestMapping("/user-events")
public class UserEventsV2Controller extends AbstractMangoRestV2Controller{

    private final BiFunction<EventInstance, User, EventInstanceModel> map;

    @Autowired
    public UserEventsV2Controller(RestModelMapper modelMapper) {
        this.map = (vo, user) -> {
            return modelMapper.map(vo, EventInstanceModel.class, user);
        };
    }

    @ApiOperation(
            value = "Query User Events",
            notes = "Query via rql in url against events for the current user",
            response=EventInstanceModel.class,
            responseContainer="Array"
            )
    @RequestMapping(method = RequestMethod.GET, value = "")
    public StreamWithTotal<EventInstanceModel> query(
            @AuthenticationPrincipal User user,
            HttpServletRequest request) {

        //Parse the RQL Query
        ASTNode query = RQLUtils.parseRQLtoAST(request.getQueryString());

        List<EventInstanceModel> events = Common.eventManager.getAllActiveUserEvents(user).stream().map(e -> {
            return map.apply(e, user);
        }).collect(Collectors.toList());

        return new FilteredStreamWithTotal<>(events, new EventFilter(query));
    }

    public static class EventFilter extends RQLFilterJavaBean<EventInstanceModel> {

        public EventFilter(ASTNode node) {
            super(node);
        }

        @Override
        protected String mapPropertyName(String propertyName) {
            if ("eventType.eventSubtype".equals(propertyName)) {
                return "eventType.subType";
            }
            return propertyName;
        }

    }

}
