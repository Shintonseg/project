package com.tible.ocm.controllers;

import com.tible.ocm.dto.RvmMachineDto;
import com.tible.ocm.dto.RvmSupplierDto;
import com.tible.ocm.models.mongo.RvmMachine;
import com.tible.ocm.models.mongo.RvmSupplier;
import com.tible.ocm.services.RvmMachineService;
import com.tible.ocm.services.RvmSupplierService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.ConversionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/rvm")
@PreAuthorize("#oauth2.hasScope('tible')")
public class RvmController {

    private final RvmMachineService rvmMachineService;
    private final RvmSupplierService rvmSupplierService;
    private final ConversionService conversionService;

    public RvmController(RvmMachineService rvmMachineService,
                         RvmSupplierService rvmSupplierService,
                         ConversionService conversionService) {
        this.rvmMachineService = rvmMachineService;
        this.rvmSupplierService = rvmSupplierService;
        this.conversionService = conversionService;
    }

    @GetMapping("/list")
    public List<RvmSupplierDto> list() {
        return rvmSupplierService.findAll().stream().map(RvmSupplierDto::from).collect(Collectors.toList());
    }

    @PostMapping("/machine/save")
    public RvmMachineDto saveRvmMachine(@RequestParam("id") RvmSupplier rvmSupplier, @RequestBody @Valid RvmMachineDto rvmMachine) {
        return RvmMachineDto.from(rvmMachineService.save(rvmSupplier, conversionService.convert(rvmMachine, RvmMachine.class)));
    }

    @GetMapping("/machine/list")
    public List<RvmMachineDto> ListMachines() {
        return rvmMachineService.findAll().stream().map(RvmMachineDto::from).collect(Collectors.toList());
    }

    @PostMapping("/supplier/save")
    public RvmSupplierDto saveRvmSupplier(@RequestBody @Valid RvmSupplierDto rvmSupplier) {
        return RvmSupplierDto.from(rvmSupplierService.save(conversionService.convert(rvmSupplier, RvmSupplier.class)));
    }

    @GetMapping("/machine/delete")
    public void deleteRvmMachine(@RequestParam("id") String id) {
        rvmMachineService.delete(id);
    }

    @GetMapping("/supplier/delete")
    public void deleteRvmSupplier(@RequestParam("id") String id) {
        rvmSupplierService.delete(id);
    }

}