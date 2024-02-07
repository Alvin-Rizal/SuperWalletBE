package com.enigma.superwallet.service.impl;

import com.enigma.superwallet.constant.ECurrencyCode;
import com.enigma.superwallet.dto.request.AccountRequest;
import com.enigma.superwallet.dto.response.AccountResponse;
import com.enigma.superwallet.dto.response.CustomerResponse;
import com.enigma.superwallet.entity.Account;
import com.enigma.superwallet.entity.Currency;
import com.enigma.superwallet.entity.Customer;
import com.enigma.superwallet.entity.UserCredential;
import com.enigma.superwallet.repository.AccountRepository;
import com.enigma.superwallet.service.AccountService;
import com.enigma.superwallet.service.CurrencyService;
import com.enigma.superwallet.service.CustomerService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final CurrencyService currencyService;
    private final CustomerService customerService;
    private final PasswordEncoder passwordEncoder;


    @Transactional(rollbackOn = Exception.class)
    @Override
    public AccountResponse createAccount(AccountRequest accountRequest) {
        System.out.println(accountRequest);
        if (accountRequest.getPin().length() < 6) return null;
        try {
            ECurrencyCode defaultCode = ECurrencyCode.IDR;

            Currency currency = Currency.builder()
                    .code(defaultCode)
                    .name(defaultCode.currencyName)
                    .build();
            currency = currencyService.getOrSaveCurrency(currency);

            int defaultNum = 100;
            int min = 1000000;
            int max = 9999999;
            Integer random =  min + (int)(Math.random() * ((max - min) + 1));

            CustomerResponse customerResponse = customerService.getById(accountRequest.getCustomerId());
            Customer customer = Customer.builder()
                    .id(customerResponse.getId())
                    .firstName(customerResponse.getFirstName())
                    .lastName(customerResponse.getLastName())
                    .phoneNumber(customerResponse.getPhoneNumber())
                    .birthDate(customerResponse.getBirthDate())
                    .gender(customerResponse.getGender())
                    .address(customerResponse.getAddress())
                    .userCredential(UserCredential.builder()
                            .email(customerResponse.getUserCredential().getEmail())
                            .build())
                    .build();

            Account account = Account.builder()
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .currency(currency)
                    .accountNumber(String.valueOf(defaultNum) + String.valueOf(random))
                    .pin(passwordEncoder.encode(accountRequest.getPin()))
                    .balance(0d)
                    .customer(customer)
                    .build();
            accountRepository.save(account);
            return AccountResponse.builder()
                    .firstName(account.getCustomer().getFirstName())
                    .accountNumber(account.getAccountNumber())
                    .currency(account.getCurrency())
                    .balance(account.getBalance())
                    .build();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Account Creation Failed");
        }
    }

    @Override
    public List<AccountResponse> getAllAccount() {
        return accountRepository.findAll().stream().map(account -> AccountResponse
                .builder()
                .firstName(account.getCustomer().getFirstName())
                .accountNumber(account.getAccountNumber())
                .balance(account.getBalance())
                .currency(account.getCurrency())
                .build()).toList();
    }

    @Override
    public AccountResponse getById(String id) {
        Account account = accountRepository.findById(id).orElse(null);
        if (account != null) {
            return AccountResponse.builder()
                    .firstName(account.getCustomer().getFirstName())
                    .accountNumber(account.getAccountNumber())
                    .currency(account.getCurrency())
                    .balance(account.getBalance())
                    .build();
        }
        return null;
    }

    @Override
    public AccountResponse createDefaultAccount(AccountRequest accountRequest) {

        return null;
    }
}