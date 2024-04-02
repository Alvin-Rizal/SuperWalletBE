package com.enigma.superwallet.service.impl;

import com.enigma.superwallet.dto.request.RegisterRequest;
import com.enigma.superwallet.dto.response.CustomerResponse;
import com.enigma.superwallet.dto.response.UserCredentialResponse;
import com.enigma.superwallet.entity.Customer;
import com.enigma.superwallet.entity.DummyBank;
import com.enigma.superwallet.entity.ProfilePicture;
import com.enigma.superwallet.entity.UserCredential;
import com.enigma.superwallet.repository.CustomerRepository;
import com.enigma.superwallet.repository.DummyBankRepository;
import com.enigma.superwallet.repository.ProfileImageRepository;
import com.enigma.superwallet.service.CustomerService;
import com.enigma.superwallet.service.UserCredentialService;
import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final UserCredentialService userCredentialService;
    private final DummyBankRepository dummyBankRepository;
    private final ProfileImageRepository profileImageRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.super_wallet.path.firebaseUrl}")
    private String firebaseJson;
    @Value("${app.super_wallet.firebase.bucketName}")
    private String bucketName;
    @Value("${app.super_wallet.firebase.downloadUrl}")
    private String DOWNLOAD_URL;

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
        try {
            Customer customer = customerRepository.findById(registerRequest.getId()).orElse(null);
            UserCredential userCredential = UserCredential.builder()
                    .id(customer.getUserCredential().getId())
                    .createdAt(customer.getCreatedAt())
                    .updatedAt(LocalDateTime.now())
                    .email(customer.getUserCredential().getEmail())
                    .password(passwordEncoder.encode(registerRequest.getPassword()))
                    .role(customer.getUserCredential().getRole())
                    .build();
            userCredentialService.updateUserCredential(userCredential);

            //Save Image to Firebase and build to Profile Picture
            String fileName = registerRequest.getProfilePictureRequest().getImage().getOriginalFilename();
            fileName = UUID.randomUUID().toString().concat(this.getExtension(fileName));

            File file = this.convertToFile(registerRequest.getProfilePictureRequest().getImage(), fileName);
            String TEMP_URL = this.uploadFile(file, fileName);
            file.delete();

            ProfilePicture profilePicture = ProfilePicture.builder().name(TEMP_URL).uploadedAt(LocalDateTime.now()).build();
            profileImageRepository.saveAndFlush(profilePicture);

            Customer customer1 = Customer.builder()
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
                    .profilePicture(profilePicture)
                    .build();

            customerRepository.save(customer1);

            return CustomerResponse.builder()
                    .firstName(customer1.getFirstName())
                    .lastName(customer1.getLastName())
                    .phoneNumber(customer1.getPhoneNumber())
                    .birthDate(customer1.getBirthDate())
                    .gender(customer1.getGender())
                    .address(customer1.getAddress())
                    .images(profilePicture.getName())
                    .userCredential(UserCredentialResponse.builder()
                            .email(userCredential.getEmail())
                            .role(userCredential.getRole().getRoleName())
                            .build())
                    .build();

        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
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

    private String uploadFile(File file, String fileName) throws IOException {
        BlobId blobId = BlobId.of(bucketName, fileName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentType("media").build();
        Credentials credentials = GoogleCredentials.fromStream(new FileInputStream(firebaseJson));
        Storage storage = StorageOptions.newBuilder().setCredentials(credentials).build().getService();
        storage.create(blobInfo, Files.readAllBytes(file.toPath()));
        return String.format(DOWNLOAD_URL, URLEncoder.encode(fileName, StandardCharsets.UTF_8));
    }

    private File convertToFile(MultipartFile multipartFile, String fileName) throws IOException {
        File tempFile = new File(fileName);
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(multipartFile.getBytes());
            fos.close();
        }
        return tempFile;
    }

    private String getExtension(String fileName) {
        return fileName.substring(fileName.lastIndexOf("."));
    }
}
