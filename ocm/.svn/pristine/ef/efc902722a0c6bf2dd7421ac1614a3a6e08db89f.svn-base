package com.tible.ocm.controllers;

import com.tible.hawk.core.utils.Utils;
import com.tible.ocm.dto.TransactionDto;
import com.tible.ocm.models.OcmTransactionResponse;
import com.tible.ocm.models.mongo.Transaction;
import com.tible.ocm.models.mongo.TransactionArticle;
import com.tible.ocm.services.TransactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/transaction")
@PreAuthorize("#oauth2.hasScope('ocm') or #oauth2.hasScope('tible')")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping("/list")
    public List<Transaction> findAll() {
        return transactionService.findAll();
    }

    @GetMapping("/byTransactionNumber")
    public List<Transaction> findByTransactionNumber(@RequestParam("transactionNumber") String transactionNumber) {
        return transactionService.findByTransactionNumber(transactionNumber);
    }

    @GetMapping("/articlesByTransactionId")
    public List<TransactionArticle> findTransactionArticlesByTransactionId(@RequestParam("transactionId") String transactionId) {
        return transactionService.findAllByTransactionId(transactionId);
    }

    @PostMapping("/create")
    public OcmTransactionResponse saveTransaction(
            @RequestBody @Valid TransactionDto transactionDto,
            HttpServletRequest request) {
        log.info("Transaction with number {} is received by REST", transactionDto.getTransactionNumber());
        return transactionService.handleTransaction(transactionDto, Utils.getRemoteAddress(request));
    }
}
