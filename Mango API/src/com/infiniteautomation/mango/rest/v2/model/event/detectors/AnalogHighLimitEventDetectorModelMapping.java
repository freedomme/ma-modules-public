/**
 * Copyright (C) 2019  Infinite Automation Software. All rights reserved.
 */
package com.infiniteautomation.mango.rest.v2.model.event.detectors;

import org.springframework.stereotype.Component;

import com.infiniteautomation.mango.rest.v2.model.RestModelMapper;
import com.infiniteautomation.mango.rest.v2.model.RestModelMapping;
import com.serotonin.m2m2.vo.User;
import com.serotonin.m2m2.vo.event.detector.AnalogHighLimitDetectorVO;

/**
 * @author Terry Packer
 *
 */
@Component
public class AnalogHighLimitEventDetectorModelMapping implements RestModelMapping<AnalogHighLimitDetectorVO, AnalogHighLimitEventDetectorModel> {

    @Override
    public Class<? extends AnalogHighLimitDetectorVO> fromClass() {
        return AnalogHighLimitDetectorVO.class;
    }

    @Override
    public Class<? extends AnalogHighLimitEventDetectorModel> toClass() {
        return AnalogHighLimitEventDetectorModel.class;
    }

    @Override
    public AnalogHighLimitEventDetectorModel map(Object from, User user, RestModelMapper mapper) {
        return new AnalogHighLimitEventDetectorModel((AnalogHighLimitDetectorVO)from);
    }

}