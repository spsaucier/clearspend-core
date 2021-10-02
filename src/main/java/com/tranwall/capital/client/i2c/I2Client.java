package com.tranwall.capital.client.i2c;

import com.tranwall.capital.client.i2c.request.GetCardStatusRequestRoot;
import com.tranwall.capital.client.i2c.response.GetCardStatusResponseRoot;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@FeignClient(name = "i2client", url = "${feign.url.i2c}")
public interface I2Client {

  @RequestMapping(
      method = RequestMethod.POST,
      value = "getCardStatus",
      produces = MediaType.APPLICATION_JSON_VALUE)
  GetCardStatusResponseRoot getCardStatusResponse(@RequestBody GetCardStatusRequestRoot request);
}
