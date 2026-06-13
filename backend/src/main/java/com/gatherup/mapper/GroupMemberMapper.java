package com.gatherup.mapper;

import com.gatherup.domain.GroupMember;
import com.gatherup.dto.response.GroupMemberResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface GroupMemberMapper {

    GroupMemberResponse toResponse(GroupMember groupMember);
}
