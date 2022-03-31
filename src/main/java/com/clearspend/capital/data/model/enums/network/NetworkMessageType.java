package com.clearspend.capital.data.model.enums.network;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum NetworkMessageType {
  AUTH_REQUEST,
  AUTH_CREATED,
  AUTH_UPDATED,
  TRANSACTION_CREATED
}
