package com.ttthinh.shoe_shop_basic.mapper;

import com.ttthinh.shoe_shop_basic.dto.response.shop.BrandResponse;
import com.ttthinh.shoe_shop_basic.entity.Brand;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BrandMapper {
    public BrandResponse toBrandResponse(Brand brand);
    public List<BrandResponse> toBrandResponse(List<Brand> brand);
}
