package com.ttthinh.shoe_shop_basic.service.impl.shopImpl;

import com.ttthinh.shoe_shop_basic.dto.request.shop.AddAddressRequest;
import com.ttthinh.shoe_shop_basic.entity.Address;
import com.ttthinh.shoe_shop_basic.entity.auth.UserAccount;
import com.ttthinh.shoe_shop_basic.exception.AppException;
import com.ttthinh.shoe_shop_basic.exception.ErrorCode;
import com.ttthinh.shoe_shop_basic.repository.shop.AddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AddressService {
    private final AddressRepository addressRepository;

    public Address getAddressById(String  id) {
        return addressRepository.findById(id).orElseThrow(
                () -> new AppException(ErrorCode.ADDRESS_NOT_FOUND)
        );
    }
    public Address getDefaultAddress(UserAccount userAccount) {
        return addressRepository.findDefaultAddressByUserId(userAccount.getId());
    }
    @Transactional
    public Address setDefaultAddress(AddAddressRequest request, UserAccount userAccount) {

        // tìm default hiện tại (nếu có)
        Address currentDefault = addressRepository.findDefaultAddressByUserId(userAccount.getId());

        boolean hasDefault = currentDefault != null;

        // CASE 1: chưa có default -> address mới luôn là default
        if (!hasDefault) {
            request.setIsDefault(true);
        }

        // CASE 2: đã có default
//        if (hasDefault && request.getIsDefault()) {
//            // unset default cũ
//            currentDefault.setIsDefault(false);
//            addressRepository.save(currentDefault);
//        }
        if (request.getIsDefault()) {
            addressRepository.clearDefaultByUserId(userAccount.getId());
        }

//        // tạo address mới
//        Address newAddress = new Address();
//        newAddress.setUserId(userAccount.getId());
//        newAddress.setProvinceId(request.getProvinceId());
//        newAddress.setWardCode(request.getWardCode());
//        newAddress.setDistrictId(request.getDistrictId());
//        newAddress.setIsDefault(request.getIsDefault());
        String fullAddress = buildFullAddress(request);
        Address newAddress = Address.builder()
                .isDefault(request.getIsDefault())
                .detailAddress(request.getDetailAddress())
                .districtId(request.getDistrictId())
                .provinceId(request.getProvinceId())
                .provinceName(request.getProvinceName())
                .districtName(request.getDistrictName())
                .wardCode(request.getWardCode())
                .wardName(request.getWardName())
                .userId(userAccount.getId())
                .fullAddress(fullAddress)
                .build();

        return addressRepository.save(newAddress);
    }
    public String buildFullAddress(AddAddressRequest req) {
        return String.join(", ",
                req.getDetailAddress(),
                req.getWardName(),
                req.getDistrictName(),
                req.getProvinceName()
        );
    }
}
