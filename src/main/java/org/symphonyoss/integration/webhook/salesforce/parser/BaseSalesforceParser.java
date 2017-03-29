/**
 * Copyright 2016-2017 Symphony Integrations - Symphony LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.symphonyoss.integration.webhook.salesforce.parser;

import static org.symphonyoss.integration.parser.ParserUtils.presentationFormat;
import static org.symphonyoss.integration.webhook.salesforce.SalesforceConstants.EMAIL_ADDRESS;
import static org.symphonyoss.integration.webhook.salesforce.SalesforceConstants.INTEGRATION_NAME;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.symphonyoss.integration.entity.Entity;
import org.symphonyoss.integration.entity.EntityBuilder;
import org.symphonyoss.integration.entity.model.User;
import org.symphonyoss.integration.messageml.MessageMLFormatConstants;
import org.symphonyoss.integration.parser.SafeString;
import org.symphonyoss.integration.service.UserService;

import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * Utility methods for Salesforce Parsers
 * Created by cmarcondes on 11/3/16.
 */
@Component
public abstract class BaseSalesforceParser implements SalesforceParser{

  public static final String LINKED_FORMATTED_TEXT = "(%s)";

  public static final String LINKED_FORMATTED = "%s";

  private static final String TIMESTAMP_FORMAT = "yyyy-MM-dd";

  @Autowired
  private UserService userService;

  private String salesforceUser;

  @Override
  public void setSalesforceUser(String user) {
    this.salesforceUser = user;
  }

  /**
   * Search the user at Symphony API.
   * @param email email to be found
   * @return User
   */
  private User getUser(String email) {
    return userService.getUserByEmail(salesforceUser, email);
  }

  protected void createMentionTagFor(Entity mainEntity, String userEntityName) {
    Entity userEntity = mainEntity.getEntityByName(userEntityName);
    if(userEntity == null){
      return;
    }
    String email = getEmail(userEntity);
    if (!StringUtils.isEmpty(email)) {
      User user = getUser(email);
      EntityBuilder.forEntity(userEntity).nestedEntity(user.getMentionEntity(INTEGRATION_NAME));
    }
  }

  /**
   * Creates the mention tag for the entity received.
   * @param mainEntity Main entity to find the nested entity
   * @param nestedEntityName Entity to find in the main entity
   * @param userEntityName Entity name for set the mention tag
   */
  protected void createListOfMentionsFor(Entity mainEntity, String nestedEntityName, String userEntityName){
    Entity entity = mainEntity.getEntityByType(nestedEntityName);
    if (entity != null) {
      for (Entity nestedEntity : entity.getEntities()) {
        createMentionTagFor(nestedEntity, userEntityName);
      }
    }
  }

  private String getEmail(Entity entity) {
    return entity.getAttributeValue(EMAIL_ADDRESS);
  }
  
  /**
   * Return the Owner Name from Salesforce json
   * @param node type JsonNode
   * @return The Owner Name if it exists formatted, null otherwise.
   */
  protected SafeString getOwnerNameFormatted(JsonNode node) {
    String ownerName = node.path("Owner").path("Name").asText();

    if (StringUtils.isEmpty(ownerName)) {
      return SafeString.EMPTY_SAFE_STRING;
    }

    return presentationFormat("Opportunity Owner: %s", ownerName);
  }

  /**
   * Return the Owner Email from Salesforce json
   * @param node type JsonNode
   * @return The Owner Email if it exists formatted, null otherwise.
   */
  protected SafeString getOwnerEmailFormatted(JsonNode node) {
    String ownerEmail = getOptionalField(node, "Owner", "Email", "").trim();

    if (StringUtils.isEmpty(ownerEmail)) {
      return SafeString.EMPTY_SAFE_STRING;
    }

    if (emailExistsInSimphony(ownerEmail.toString())) {
      return presentationFormat(LINKED_FORMATTED, presentationFormat(MessageMLFormatConstants.MESSAGEML_MENTION_EMAIL_FORMAT, ownerEmail));
    }

    return presentationFormat(LINKED_FORMATTED_TEXT, ownerEmail);
  }

  /**
   * Return the Type from Salesforce json
   * @param node type JsonNode
   * @return The Type if it exists formatted, null otherwise.
   */
  protected SafeString getTypeFormatted(JsonNode node) {
    String type = node.path("Type").asText();

    if (StringUtils.isEmpty(type)) {
      return SafeString.EMPTY_SAFE_STRING;
    }

    return presentationFormat("Type: %s", type);
  }

  /**
   * Return the Stage Name from Salesforce json
   * @param node type JsonNode
   * @return The Stage Name if it exists formatted, null otherwise.
   */
  protected SafeString getStageNameFormatted(JsonNode node) {
    String stageName = node.path("StageName").asText();

    if (StringUtils.isEmpty(stageName)) {
      return SafeString.EMPTY_SAFE_STRING;
    }

    return presentationFormat("Stage: %s", stageName);
  }

  /**
   * Return the Close Date from Salesforce json
   * @param node type JsonNode
   * @return The Close Date if it exists formatted, null otherwise.
   */
  protected SafeString getCloseDateFormatted(JsonNode node) {
    String closeDate = node.path("CloseDate").asText();

    if (StringUtils.isEmpty(closeDate)) {
      return SafeString.EMPTY_SAFE_STRING;
    }

    SimpleDateFormat formatter = new SimpleDateFormat(TIMESTAMP_FORMAT);
    String closeDateFormat;
    try {
      closeDateFormat = formatter.format(formatter.parse(closeDate));
    } catch (ParseException e) {
      return SafeString.EMPTY_SAFE_STRING;
    }

    return presentationFormat("Close Date: %s", closeDateFormat);
  }

  /**
   * Return the Account Name from Salesforce json
   * @param node type JsonNode
   * @return The Account Name if it exists formatted, null otherwise.
   */
  protected SafeString getAccountNameFormatted(JsonNode node) {
    String accountName = node.path("Account").path("Name").asText();

    if (StringUtils.isEmpty(accountName)) {
      return SafeString.EMPTY_SAFE_STRING;
    }

    return presentationFormat("Account Name: %s", accountName);
  }

  /**
   * Return the URL from Account json formated.
   * @param node type JsonNode
   * @return (<a href="https://symdev1-dev-ed.my.salesforce.com/00146000004oPCcAAM"/>)
   */
  protected SafeString getAccountLinkedFormatted(JsonNode node) {
    String accountLink = node.path("Account").path("Link").asText();

    if (accountLink.isEmpty()) {
      return SafeString.EMPTY_SAFE_STRING;
    }

    SafeString finalUrl = presentationFormat(MessageMLFormatConstants.MESSAGEML_LINK_HREF_FORMAT, accountLink.toString());

    return presentationFormat(LINKED_FORMATTED_TEXT, finalUrl);
  }

  /**
   * Return the Amount from Salesforce json
   * @param node type JsonNode
   * @return The Amount if it exists formatted, null otherwise.
   */
  protected SafeString getAmountFormatted(JsonNode node) {
    String amount = node.path("Amount").asText();

    if (StringUtils.isEmpty(amount)) {
      return SafeString.EMPTY_SAFE_STRING;
    }

    return presentationFormat("Amount: %s", amount);
  }

  /**
   * Return the Next Step from Salesforce json
   * @param node type JsonNode
   * @return The Next Step if it exists formatted, null otherwise.
   */
  protected SafeString getNextStepFormatted(JsonNode node) {
    String nextStep = getOptionalField(node,  "NextStep", "-").trim();

    if (StringUtils.isEmpty(nextStep)) {
      return SafeString.EMPTY_SAFE_STRING;
    }

    return presentationFormat("Next Step: %s", nextStep);
  }

  /**
   * Return the Probability from Salesforce json
   * @param node type JsonNode
   * @return The Probability if it exists formatted, null otherwise.
   */
  protected SafeString getProbabilityFormatted(JsonNode node) {
    String probability = node.path("Probability").asText();

    if (StringUtils.isEmpty(probability)) {
      return SafeString.EMPTY_SAFE_STRING;
    }

    return presentationFormat("Probability: %s", probability);
  }

  /**
   * Return the CurrencyIsoCode from Salesforce json
   * @param node type JsonNode
   * @return The CurrencyIsoCode if it exists formatted, null otherwise.
   */
  protected SafeString getCurrencyIsoCode(JsonNode node) {
    String currencyIsoCode = node.path("CurrencyIsoCode").asText();

    if (StringUtils.isEmpty(currencyIsoCode)) {
      return SafeString.EMPTY_SAFE_STRING;
    }

    return presentationFormat("%s", currencyIsoCode);
  }

  private String getOptionalField(JsonNode node, String path, String key, String defaultValue) {
    String value = node.path(path).path(key).asText();

    if (value.isEmpty()) {
      return defaultValue;
    }

    return value;
  }

  private String getOptionalField(JsonNode node, String key, String defaultValue) {
    String value = node.path(key).asText();

    if (value.isEmpty()) {
      return defaultValue;
    }

    return value;
  }

  /**
   * Verified if already exists email address
   * @param emailAddress
   * @return
   */
  private boolean emailExistsInSimphony(String emailAddress) {
    if ((emailAddress == null) || (emailAddress.isEmpty())) {
      return false;
    }

    User user = userService.getUserByEmail(salesforceUser, emailAddress);
    if (user.getId() == null) {
      return false;
    }

    return true;
  }

}