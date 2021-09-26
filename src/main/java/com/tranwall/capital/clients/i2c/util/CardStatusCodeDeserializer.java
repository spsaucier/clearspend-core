package com.tranwall.capital.clients.i2c.util;

import com.tranwall.capital.clients.i2c.CardStatusCode;

import java.util.function.Function;

public class CardStatusCodeDeserializer extends I2CEnumDeserializer<CardStatusCode> {

    @Override
    protected Function<String, CardStatusCode> getDeserializerFunction() {
        return CardStatusCode::fromCode;
    }
}
