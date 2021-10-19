package com.tranwall.capital.controller;

import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.controller.type.bin.Bin;
import com.tranwall.capital.controller.type.bin.CreateBinRequest;
import com.tranwall.capital.controller.type.bin.CreateBinResponse;
import com.tranwall.capital.service.BinService;
import io.swagger.annotations.ApiParam;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bins")
@RequiredArgsConstructor
public class BinController {

  private final BinService binService;

  @PostMapping("")
  private CreateBinResponse createBin(@RequestBody @Validated CreateBinRequest request) {
    return new CreateBinResponse(binService.createBin(request.getBin(), request.getName()).getId());
  }

  @GetMapping("")
  private List<Bin> getBins() {
    return binService.findAllBins().stream()
        .map(e -> new Bin(e.getId(), e.getBin(), e.getName()))
        .collect(Collectors.toList());
  }

  @GetMapping("{binOrBinId}")
  private Bin getBin(
      @PathVariable(value = "binOrBinId")
          @ApiParam(
              required = true,
              name = "binOrBinId",
              value = "A BIN or the ID of the bin record.",
              example = "456123")
          String binOrBinId) {
    if (binOrBinId.length() == 6) {
      return new Bin(binService.retrieveBin(binOrBinId));
    }

    return new Bin(binService.retrieveBin(new TypedId<>(binOrBinId)));
  }
}
