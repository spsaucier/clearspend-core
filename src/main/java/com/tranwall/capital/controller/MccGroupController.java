package com.tranwall.capital.controller;

import com.tranwall.capital.controller.type.CurrentUser;
import com.tranwall.capital.controller.type.mcc.MccGroup;
import com.tranwall.capital.service.MccGroupService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mcc-groups")
@RequiredArgsConstructor
public class MccGroupController {

  private final MccGroupService mccGroupService;

  @GetMapping
  private List<MccGroup> getGroups() {
    return mccGroupService.retrieveMccGroups(CurrentUser.get().businessId()).stream()
        .map(MccGroup::of)
        .collect(Collectors.toList());
  }
}
