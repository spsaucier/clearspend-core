package com.clearspend.capital.controller;

import com.clearspend.capital.data.model.enums.MccGroup;
import java.util.EnumSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mcc-groups")
@RequiredArgsConstructor
public class MccGroupController {

  @GetMapping
  Set<MccGroup> getGroups() {
    return EnumSet.allOf(MccGroup.class);
  }
}
