package com.gatherup.mapper;

import com.gatherup.domain.JoinRequest;
import com.gatherup.dto.response.JoinRequestResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {EventMapper.class, UserMapper.class})
public interface JoinRequestMapper {

    @Mapping(target = "event",     source = "event")
    @Mapping(target = "requester", source = "requester")
    JoinRequestResponse toResponse(JoinRequest joinRequest);
}
