package com.example.ashore.framework.desensitize.core.slider.handler;

import com.example.ashore.framework.desensitize.core.slider.annotation.TelephoneDesensitize;

/**
 * {@link TelephoneDesensitize} 的脱敏处理器
 */
public class TelephoneDesensitization extends AbstractSliderDesensitizationHandler<TelephoneDesensitize> {

    @Override
    Integer getPrefixKeep(TelephoneDesensitize annotation) {
        return annotation.prefixKeep();
    }

    @Override
    Integer getSuffixKeep(TelephoneDesensitize annotation) {
        return annotation.suffixKeep();
    }

    @Override
    String getReplacer(TelephoneDesensitize annotation) {
        return annotation.replacer();
    }

}
