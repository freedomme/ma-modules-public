/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.infiniteautomation.mango.rest.v2.model.event.detectors.rt;

import com.serotonin.m2m2.rt.event.detectors.AlphanumericRegexStateDetectorRT;
import com.serotonin.m2m2.vo.event.detector.AlphanumericRegexStateDetectorVO;

/**
 *
 * @author Terry Packer
 */
public class AlphanumericRegexStateEventDetectorRTModel extends StateDetectorRTModel<AlphanumericRegexStateDetectorVO>{

    public AlphanumericRegexStateEventDetectorRTModel(AlphanumericRegexStateDetectorRT rt) {
        super(rt);
    }

}
