/**
 * Copyright (C) 2020  Infinite Automation Software. All rights reserved.
 */

package com.infiniteautomation.mango.rest.v2.model.mailingList;

import org.springframework.stereotype.Component;

import com.infiniteautomation.mango.rest.v2.model.RestModelMapper;
import com.infiniteautomation.mango.rest.v2.model.RestModelMapping;
import com.serotonin.m2m2.vo.mailingList.AddressEntry;
import com.serotonin.m2m2.vo.permission.PermissionHolder;

/**
 *
 * @author Terry Packer
 */
@Component
public class AddressEntryModelMapping implements RestModelMapping<AddressEntry, AddressEntryModel> {

    @Override
    public Class<? extends AddressEntry> fromClass() {
        return AddressEntry.class;
    }

    @Override
    public Class<? extends AddressEntryModel> toClass() {
        return AddressEntryModel.class;
    }

    @Override
    public AddressEntryModel map(Object from, PermissionHolder user, RestModelMapper mapper) {
        AddressEntry vo = (AddressEntry)from;
        return new AddressEntryModel(vo);
    }

}
