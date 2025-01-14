package com.enigma.superwallet.controllers;

import com.enigma.superwallet.constant.AppPath;
import com.enigma.superwallet.dto.response.CurrencyHistoryResponse;
import com.enigma.superwallet.dto.response.DefaultResponse;
import com.enigma.superwallet.dto.response.ErrorResponse;
import com.enigma.superwallet.service.CurrencyHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping(AppPath.CURRENCY)
public class CurrencyHistoryController {
    private final CurrencyHistoryService currencyHistoryService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_SUPER_ADMIN')")
    public ResponseEntity<?> getCurrencyHistory(
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam("baseCurrency") String baseCurrency
    ) {
        try {

            List<CurrencyHistoryResponse> currencyHistoryList = currencyHistoryService.getCurrencyHistoryByDateAndBaseCurrency(date.toString(), baseCurrency);
            return ResponseEntity.status(HttpStatus.OK).body(DefaultResponse.builder()
                    .message("Success get history currency rate")
                    .statusCode(HttpStatus.OK.value())
                    .data(currencyHistoryList)
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ErrorResponse.builder()
                            .statusCode(HttpStatus.BAD_REQUEST.value())
                            .message(e.getMessage())
                            .build());
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(ErrorResponse.builder()
                            .statusCode(e.getStatusCode().value())
                            .message(e.getMessage())
                            .build());
        }
    }

    @GetMapping("/get")
    public ResponseEntity<?> getRateHistory(@RequestParam String baseCurrency, @RequestParam String targetCurrency) {
        try {
            CurrencyHistoryResponse result = currencyHistoryService.getCurrencyRate(baseCurrency, targetCurrency);

            return ResponseEntity.status(HttpStatus.OK).body(DefaultResponse.builder()
                    .message("Success get history currency rate")
                    .statusCode(HttpStatus.OK.value())
                    .data(result)
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ErrorResponse.builder()
                            .statusCode(HttpStatus.BAD_REQUEST.value())
                            .message(e.getMessage())
                            .build());
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(ErrorResponse.builder()
                            .statusCode(e.getStatusCode().value())
                            .message(e.getMessage())
                            .build());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ErrorResponse.builder()
                            .statusCode(HttpStatus.NOT_FOUND.value())
                            .message(e.getMessage())
                            .build());
        }
    }
}
