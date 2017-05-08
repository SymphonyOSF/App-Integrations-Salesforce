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

package org.symphonyoss.integration.webhook.salesforce;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.symphonyoss.integration.model.config.IntegrationSettings;
import org.symphonyoss.integration.model.message.Message;
import org.symphonyoss.integration.webhook.WebHookIntegration;
import org.symphonyoss.integration.webhook.WebHookPayload;
import org.symphonyoss.integration.webhook.exception.WebHookParseException;
import org.symphonyoss.integration.webhook.parser.WebHookParser;
import org.symphonyoss.integration.webhook.salesforce.parser.SalesforceParserFactory;
import org.symphonyoss.integration.webhook.salesforce.parser.SalesforceParserResolver;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MediaType;

/**
 *
 * Implementation of a WebHook to integrate with SALESFORCE, rendering it's messages.
 *
 * This integration class should support MessageML v1 and MessageML v2 according to the Agent Version.
 *
 * There is a component {@link SalesforceParserResolver} responsible to identify the correct factory should
 * be used to build the parsers according to the MessageML supported.
 *
 * Created by rsanchez on 31/08/16.
 */
@Component
public class SalesforceWebHookIntegration extends WebHookIntegration {

  @Autowired
  private SalesforceParserResolver salesforceParserResolver;

  @Autowired
  private List<SalesforceParserFactory> factories;

  /**
   * Callback to update the integration settings in the parser classes.
   * @param settings Integration settings
   */
  @Override
  public void onConfigChange(IntegrationSettings settings) {
    super.onConfigChange(settings);

    for (SalesforceParserFactory factory : factories) {
      factory.onConfigChange(settings);
    }
  }

  /**
   * Parse message received from SALESFORCE according to the event type and MessageML version supported.
   * @param input Message received from SALESFORCE
   * @return Message to be posted
   * @throws WebHookParseException Failure to parse the incoming payload
   */
  @Override
  public Message parse(WebHookPayload input) throws WebHookParseException {
    WebHookParser parser = salesforceParserResolver.getFactory().getParser(input);
    return parser.parse(input);
  }

  /**
   * @see WebHookIntegration#getSupportedContentTypes()
   */
  @Override
  public List<MediaType> getSupportedContentTypes() {
    List<MediaType> supportedContentTypes = new ArrayList<>();
    supportedContentTypes.add(MediaType.WILDCARD_TYPE);
    return supportedContentTypes;
  }
}