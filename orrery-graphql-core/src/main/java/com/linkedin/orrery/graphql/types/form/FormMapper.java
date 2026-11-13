package com.linkedin.orrery.graphql.types.form;

import static com.linkedin.metadata.Constants.FORM_INFO_ASPECT_NAME;
import static com.linkedin.metadata.Constants.OWNERSHIP_ASPECT_NAME;
import static com.linkedin.metadata.utils.UnknownDataHandlings.handling;

import com.linkedin.common.Ownership;
import com.linkedin.common.urn.Urn;
import com.linkedin.data.DataMap;
import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.generated.CorpGroup;
import com.linkedin.orrery.graphql.generated.CorpUser;
import com.linkedin.orrery.graphql.generated.EntityType;
import com.linkedin.orrery.graphql.generated.Form;
import com.linkedin.orrery.graphql.generated.FormActorAssignment;
import com.linkedin.orrery.graphql.generated.FormInfo;
import com.linkedin.orrery.graphql.generated.FormPrompt;
import com.linkedin.orrery.graphql.generated.FormPromptType;
import com.linkedin.orrery.graphql.generated.FormType;
import com.linkedin.orrery.graphql.generated.OwnershipTypeEntity;
import com.linkedin.orrery.graphql.generated.StructuredPropertyEntity;
import com.linkedin.orrery.graphql.generated.StructuredPropertyParams;
import com.linkedin.orrery.graphql.types.common.mappers.OwnershipMapper;
import com.linkedin.orrery.graphql.types.common.mappers.util.MappingHelper;
import com.linkedin.orrery.graphql.types.common.mappers.util.UnknownEnumValues;
import com.linkedin.orrery.graphql.types.mappers.ModelMapper;
import com.linkedin.entity.EntityResponse;
import com.linkedin.entity.EnvelopedAspectMap;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class FormMapper implements ModelMapper<EntityResponse, Form> {

  public static final FormMapper INSTANCE = new FormMapper();

  public static Form map(@Nullable final QueryContext context, @Nonnull final EntityResponse form) {
    return INSTANCE.apply(context, form);
  }

  public Form apply(
      @Nullable final QueryContext context, @Nonnull final EntityResponse entityResponse) {
    Form result = new Form();
    Urn entityUrn = entityResponse.getUrn();
    result.setUrn(entityUrn.toString());
    result.setType(EntityType.FORM);

    EnvelopedAspectMap aspectMap = entityResponse.getAspects();
    MappingHelper<Form> mappingHelper = new MappingHelper<>(aspectMap, result);
    mappingHelper.mapToResult(FORM_INFO_ASPECT_NAME, this::mapFormInfo);
    mappingHelper.mapToResult(
        OWNERSHIP_ASPECT_NAME,
        (form, dataMap) ->
            form.setOwnership(OwnershipMapper.map(context, new Ownership(dataMap), entityUrn)));

    return mappingHelper.getResult();
  }

  private void mapFormInfo(@Nonnull Form form, @Nonnull DataMap dataMap) {
    com.linkedin.form.FormInfo gmsFormInfo = new com.linkedin.form.FormInfo(dataMap);
    FormInfo formInfo = new FormInfo();
    formInfo.setName(gmsFormInfo.getName());
    // A form type only a newer version knows shows as COMPLETION (the schema's default), not the
    // stronger VERIFICATION.
    formInfo.setType(
        UnknownEnumValues.map(FormType.class, gmsFormInfo.getType(), FormType.COMPLETION));
    if (gmsFormInfo.hasDescription()) {
      formInfo.setDescription(gmsFormInfo.getDescription());
    }
    formInfo.setPrompts(this.mapFormPrompts(gmsFormInfo, form.getUrn()));
    formInfo.setActors(mapFormActors(gmsFormInfo.getActors()));
    form.setInfo(formInfo);
  }

  private List<FormPrompt> mapFormPrompts(
      @Nonnull com.linkedin.form.FormInfo gmsFormInfo, @Nonnull String formUrn) {
    List<FormPrompt> formPrompts = new ArrayList<>();
    if (gmsFormInfo.hasPrompts()) {
      gmsFormInfo
          .getPrompts()
          .forEach(
              FormPrompt -> {
                // Prompt types only a newer version knows (e.g. after a rollback) are skipped.
                if (!handling().toleratesUnknownEnum()
                    || UnknownEnumValues.mapOrNull(FormPromptType.class, FormPrompt.getType())
                        != null) {
                  formPrompts.add(mapFormPrompt(FormPrompt, formUrn));
                }
              });
    }
    return formPrompts;
  }

  private FormPrompt mapFormPrompt(
      @Nonnull com.linkedin.form.FormPrompt gmsFormPrompt, @Nonnull String formUrn) {
    final FormPrompt formPrompt = new FormPrompt();
    formPrompt.setId(gmsFormPrompt.getId());
    formPrompt.setTitle(gmsFormPrompt.getTitle());
    formPrompt.setType(FormPromptType.valueOf(gmsFormPrompt.getType().toString()));
    formPrompt.setRequired(gmsFormPrompt.isRequired());
    formPrompt.setFormUrn(formUrn);
    if (gmsFormPrompt.hasDescription()) {
      formPrompt.setDescription(gmsFormPrompt.getDescription());
    }

    if (gmsFormPrompt.hasStructuredPropertyParams()) {
      final StructuredPropertyParams params = new StructuredPropertyParams();
      final Urn structuredPropUrn = gmsFormPrompt.getStructuredPropertyParams().getUrn();
      final StructuredPropertyEntity structuredProp = new StructuredPropertyEntity();
      structuredProp.setUrn(structuredPropUrn.toString());
      structuredProp.setType(EntityType.STRUCTURED_PROPERTY);
      params.setStructuredProperty(structuredProp);
      formPrompt.setStructuredPropertyParams(params);
    }

    return formPrompt;
  }

  private FormActorAssignment mapFormActors(com.linkedin.form.FormActorAssignment gmsFormActors) {
    FormActorAssignment result = new FormActorAssignment();
    result.setOwners(gmsFormActors.isOwners());
    if (gmsFormActors.hasOwnershipTypes()) {
      result.setOwnershipTypes(
          gmsFormActors.getOwnershipTypes().stream()
              .map(this::mapOwnershipType)
              .collect(Collectors.toList()));
    }
    if (gmsFormActors.hasUsers()) {
      result.setUsers(
          gmsFormActors.getUsers().stream().map(this::mapUser).collect(Collectors.toList()));
    }
    if (gmsFormActors.hasGroups()) {
      result.setGroups(
          gmsFormActors.getGroups().stream().map(this::mapGroup).collect(Collectors.toList()));
    }
    return result;
  }

  private OwnershipTypeEntity mapOwnershipType(Urn ownershipTypeUrn) {
    OwnershipTypeEntity ownershipType = new OwnershipTypeEntity();
    ownershipType.setUrn(ownershipTypeUrn.toString());
    ownershipType.setType(EntityType.CUSTOM_OWNERSHIP_TYPE);
    return ownershipType;
  }

  private CorpUser mapUser(Urn userUrn) {
    CorpUser user = new CorpUser();
    user.setUrn(userUrn.toString());
    return user;
  }

  private CorpGroup mapGroup(Urn groupUrn) {
    CorpGroup group = new CorpGroup();
    group.setUrn(groupUrn.toString());
    return group;
  }
}
