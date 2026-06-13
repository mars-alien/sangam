package com.gatherup.mapper;

import com.gatherup.domain.Event;
import com.gatherup.dto.response.EventDetailResponse;
import com.gatherup.dto.response.EventSummaryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface EventMapper {

    @Mapping(target = "distanceKm", ignore = true)
    @Mapping(target = "latitude",  ignore = true)
    @Mapping(target = "longitude", ignore = true)
    @Mapping(
        target = "full",
        expression = "java(event.getCurrentMemberCount() >= event.getMaxCompanions())"
    )
    EventSummaryResponse toSummaryResponse(Event event);

    @Mapping(target = "distanceKm", ignore = true)
    @Mapping(
        target = "full",
        expression = "java(event.getCurrentMemberCount() >= event.getMaxCompanions())"
    )
    @Mapping(
        target = "latitude",
        expression = "java(event.getLocation() != null ? event.getLocation().getY() : null)"
    )
    @Mapping(
        target = "longitude",
        expression = "java(event.getLocation() != null ? event.getLocation().getX() : null)"
    )
    @Mapping(target = "isCreator",         ignore = true)
    @Mapping(target = "currentUserStatus", ignore = true)
    @Mapping(target = "waitlistPosition",  ignore = true)
    EventDetailResponse toDetailResponse(Event event);
}
