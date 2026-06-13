package com.gatherup.mapper;

import com.gatherup.domain.User;
import com.gatherup.dto.response.UserProfileResponse;
import com.gatherup.dto.response.UserSummaryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserSummaryResponse toSummaryResponse(User user);

    @Mapping(
        target = "totalEventsCreated",
        expression = "java(user.getCreatedEvents().size())"
    )
    UserProfileResponse toProfileResponse(User user);
}
