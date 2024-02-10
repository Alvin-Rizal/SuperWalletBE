package com.enigma.superwallet.service.impl;

import com.enigma.superwallet.dto.request.RegisterRequest;
import com.enigma.superwallet.dto.response.CustomerResponse;
import com.enigma.superwallet.dto.response.UserCredentialResponse;
import com.enigma.superwallet.entity.Customer;
import com.enigma.superwallet.entity.DummyBank;
import com.enigma.superwallet.entity.UserCredential;
import com.enigma.superwallet.repository.CustomerRepository;
import com.enigma.superwallet.repository.DummyBankRepository;
import com.enigma.superwallet.service.CustomerService;
import com.enigma.superwallet.service.UserCredentialService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final UserCredentialService userCredentialService;
    private final PasswordEncoder passwordEncoder;
    private final DummyBankRepository dummyBankRepository;


    @Override
    public CustomerResponse createCustomer(Customer customer) {
        Customer customer1 = customerRepository.saveAndFlush(customer);
        return CustomerResponse.builder()
                .id(customer1.getId())
                .firstName(customer1.getFirstName())
                .lastName(customer1.getLastName())
                .build();
    }

    @Override
    public List<CustomerResponse> getAll() {
        return customerRepository.findAll().stream()
                .filter(Customer::getIsActive)
                .map(customer -> CustomerResponse.builder()
                        .id(customer.getId())
                        .firstName(customer.getFirstName())
                        .lastName(customer.getLastName())
                        .phoneNumber(customer.getPhoneNumber())
                        .birthDate(customer.getBirthDate())
                        .gender(customer.getGender())
                        .address(customer.getAddress())
                        .userCredential(UserCredentialResponse.builder()
                                .email(customer.getUserCredential().getEmail())
                                .role(customer.getUserCredential().getRole().getRoleName())
                                .build())
                        .build())
                .toList();
    }

    @Override
    public CustomerResponse getById(String id) {
        Customer customer = customerRepository.findById(id).orElse(null);
        if (customer != null && customer.getIsActive()) {
            return CustomerResponse.builder()
                    .id(customer.getId())
                    .firstName(customer.getFirstName())
                    .lastName(customer.getLastName())
                    .phoneNumber(customer.getPhoneNumber())
                    .birthDate(customer.getBirthDate())
                    .address(customer.getAddress())
                    .gender(customer.getGender())
                    .userCredential(UserCredentialResponse.builder()
                            .email(customer.getUserCredential().getEmail())
                            .role(customer.getUserCredential().getRole().getRoleName())
                            .build())
                    .build();
        }
        return null;
    }

    @Transactional(rollbackOn = Exception.class)
    @Override
    public CustomerResponse update(RegisterRequest registerRequest) {
        Customer customer = customerRepository.findById(registerRequest.getId()).orElse(null);
        if (customer != null) {
            UserCredential userCredential = UserCredential.builder()
                    .id(customer.getUserCredential().getId())
                    .createdAt(customer.getUserCredential().getCreatedAt())
                    .updatedAt(LocalDateTime.now())
                    .email(registerRequest.getEmail())
                    .password(passwordEncoder.encode(registerRequest.getPassword()))
                    .role(customer.getUserCredential().getRole())
                    .build();
            userCredentialService.updateUserCredential(userCredential);
            Customer updatedCustomer = Customer.builder()
                    .id(registerRequest.getId())
                    .createdAt(customer.getCreatedAt())
                    .updatedAt(LocalDateTime.now())
                    .firstName(registerRequest.getFirstName())
                    .lastName(registerRequest.getLastName())
                    .phoneNumber(registerRequest.getPhoneNumber())
                    .birthDate(LocalDate.parse(registerRequest.getBirthDate()))
                    .gender(registerRequest.getGender())
                    .address(registerRequest.getAddress())
                    .isActive(customer.getIsActive())
                    .userCredential(userCredential)
                    .build();
            customerRepository.save(updatedCustomer);
            return CustomerResponse.builder()
                    .id(updatedCustomer.getId())
                    .firstName(updatedCustomer.getFirstName())
                    .lastName(updatedCustomer.getLastName())
                    .phoneNumber(updatedCustomer.getPhoneNumber())
                    .birthDate(updatedCustomer.getBirthDate())
                    .gender(updatedCustomer.getGender())
                    .address(updatedCustomer.getAddress())
                    .userCredential(UserCredentialResponse.builder()
                            .email(userCredential.getEmail())
                            .role(userCredential.getRole().getRoleName())
                            .build())
                    .build();
        }
        return null;
    }

    @Override
    public Boolean delete(String id) {
        Customer customer = customerRepository.findById(id).orElse(null);
        if (customer != null) {
            Customer deletedCustomer = Customer.builder()
                    .id(customer.getId())
                    .createdAt(customer.getCreatedAt())
                    .updatedAt(LocalDateTime.now())
                    .firstName(customer.getFirstName())
                    .lastName(customer.getLastName())
                    .phoneNumber(customer.getPhoneNumber())
                    .birthDate(customer.getBirthDate())
                    .gender(customer.getGender())
                    .address(customer.getAddress())
                    .isActive(false)
                    .userCredential(customer.getUserCredential())
                    .build();
            customerRepository.save(deletedCustomer);
            return true;
        }
        return false;
    }

    @Override
    public Optional<Customer> getCustomerByUserCredentialId(String id) {
        return customerRepository.findByUserCredentialId(id);
    }

    @Transactional
    @Override
    public void updateDummyBankId(String customerId, String dummyBankId) {
        // Fetch customer by ID
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found with ID: " + customerId));

        // Fetch dummy bank by ID
        DummyBank dummyBank = dummyBankRepository.findById(dummyBankId)
                .orElseThrow(() -> new RuntimeException("Dummy bank not found with ID: " + dummyBankId));

        // Set the dummy bank for the customer
        customer.setDummyBank(dummyBank);

        // Save the updated customer entity
        customerRepository.save(customer);
    }
}
