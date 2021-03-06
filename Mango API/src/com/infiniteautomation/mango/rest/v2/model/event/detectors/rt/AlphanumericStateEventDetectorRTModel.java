/**
 * Copyright (C) 2017 Infinite Automation Software. All rights reserved.
 *
 */
package com.infiniteautomation.mango.rest.v2.model.event.detectors.rt;

import com.serotonin.m2m2.rt.event.detectors.AlphanumericStateDetectorRT;
import com.serotonin.m2m2.vo.event.detector.AlphanumericStateDetectorVO;

/**
 *
 * @author Terry Packer
 */
public class AlphanumericStateEventDetectorRTModel extends StateDetectorRTModel<AlphanumericStateDetectorVO>{

    public AlphanumericStateEventDetectorRTModel(AlphanumericStateDetectorRT rt) {
        super(rt);
    }

}
