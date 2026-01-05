package com.ttthinh.shoe_shop_basic.service;

import com.ttthinh.shoe_shop_basic.dto.request.LoginRequest;
import com.ttthinh.shoe_shop_basic.dto.request.RegisterRequest;
import com.ttthinh.shoe_shop_basic.dto.response.UserResponse;

import java.util.List;

public interface UserService {
    UserResponse register(RegisterRequest registerRequest);
    List<UserResponse> getAllUsers();
    UserResponse getMyInformation();
    UserResponse getMyInformationById(String id);
    void deleteAllUsers();
}
