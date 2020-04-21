/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
 */

package com.infiniteautomation.mango.rest.v2.model.role;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.infiniteautomation.mango.rest.v2.model.RestModelMapper;
import com.infiniteautomation.mango.rest.v2.model.RestModelMapping;
import com.infiniteautomation.mango.util.exception.ValidationException;
import com.serotonin.m2m2.Common;
import com.serotonin.m2m2.db.dao.RoleDao;
import com.serotonin.m2m2.vo.permission.PermissionHolder;
import com.serotonin.m2m2.vo.role.Role;
import com.serotonin.m2m2.vo.role.RoleVO;

/**
 *
 * @author Terry Packer
 */
@Component
public class RoleModelMapping implements RestModelMapping<RoleVO, RoleModel> {

    private final RoleDao dao;

    @Autowired
    public RoleModelMapping(RoleDao dao) {
        this.dao = dao;
    }

    @Override
    public Class<? extends RoleVO> fromClass() {
        return RoleVO.class;
    }

    @Override
    public Class<? extends RoleModel> toClass() {
        return RoleModel.class;
    }

    @Override
    public RoleModel map(Object from, PermissionHolder user, RestModelMapper mapper) {
        return new RoleModel((RoleVO)from);
    }

    @Override
    public RoleVO unmap(Object from, PermissionHolder user, RestModelMapper mapper) throws ValidationException {
        RoleModel model = (RoleModel)from;
        RoleVO vo = model.toVO();
        Set<Role> roles = new HashSet<>();
        for(String xid : model.getInherited()) {
            Integer id = dao.getIdByXid(xid);
            roles.add(new Role(id == null ? Common.NEW_ID : id, xid));
        }
        return vo;
    }

}
