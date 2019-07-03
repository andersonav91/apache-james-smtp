**Mailet configuration example**

**Add the unsubscribe footer in the emails**
 
```html
 <mailet match="org.glx.matchers.GLXUnsubscribedEmails" class="Null">
 </mailet>
	  
 <mailet match="All" class="org.glx.mailets.GLXUnsubscribe">
 </mailet>
```

**Dispatcher configuration**

_Dispatchers are the ones in charge of redirecting emails from an account thru a set of gateways. They also handle the limits on each gateway._

```html
 <mailet match="SenderIs={SENDING_MAIL}" class="org.glx.mailets.GLXGatewayDispatcher">
 <gateways>{GATEWAY_ID_1},{GATEWAY_ID_2}</gateways>
 <max_emails_per_day_gateway_1>{MAX_EMAIL_GATEWAY_1}</max_emails_per_day_gateway_1>
 <max_emails_per_day_gateway_2>{MAX_EMAIL_GATEWAY_2}</max_emails_per_day_gateway_2>
 </mailet>
```

**Sendgrid Relay**

```html
 <mailet match="HasMailAttributeWithValue=currentGateway,{GATEWAY_ID}" class="org.glx.mailets.GLXSendgridRelay">
 <sendgrid_api_key>{SENDGRID_API_KEY}</sendgrid_api_key>
 <sendgrid_email_from>{EMAIL_FROM}</sendgrid_email_from>
 <gateway_id>{GATEWAY_ID}</gateway_id>
 </mailet>
```

**Amazon SES Relay**

```html
 <mailet match="HasMailAttributeWithValue=currentGateway,{GATEWAY_ID}" class="org.glx.mailets.GLXAmazonSESRelay">
 <amazon_ses_email_from>{EMAIL_FROM}</amazon_ses_email_from>
 <amazon_access_key>{AMAZON_ACCESS_KEY}</amazon_access_key>
 <amazon_secret_key>{AMAZON_SECRET_KEY}</amazon_secret_key>
 <amazon_region>{AMAZON_REGION}</amazon_region>
 <gateway_id>{GATEWAY_ID}</gateway_id>
 </mailet>
```
 
 
**Gmail Relay**
 
```html
 <mailet match="HasMailAttributeWithValue=currentGateway,{GATEWAY_ID}" class="org.glx.mailets.GLXGmailRelay">
 <gmail_email_from>{GMAIL_ACCOUNT}</gmail_email_from>
 <gmail_email_stmp>{STMP_URL}</gmail_email_stmp>
 <gmail_email_port>{STMP_PORT}</gmail_email_port>
 <gmail_email_password>{GMAIL_PASSWORD}</gmail_email_password>
 <gateway_id>{GATEWAY_ID}</gateway_id>
 </mailet>
```
 
**Mailchimp Relay**
  
```html
 <mailet  match="HasMailAttributeWithValue=currentGateway,{GATEWAY_ID}" class="org.glx.mailets.GLXMailchimpRelay">
 <mailchimp_email_from>{EMAIL_FROM}</mailchimp_email_from>
 <mailchimp_api_key>{MAILCHIMP_API_KEY}</mailchimp_api_key>
 <mailchimp_data_center>{MAILCHIMP_DATA_CENTER}</mailchimp_data_center>
 <mailchimp_company>{MAILCHIMP_COMPANY_NAME}</mailchimp_company>
 <mailchimp_address1>{MAILCHIMP_COMPANY_ADDRESS}</mailchimp_address1>
 <mailchimp_city>{MAILCHIMP_COMPANY_CITY}</mailchimp_city>
 <mailchimp_state>{MAILCHIMP_COMPANY_STATE}</mailchimp_state>
 <mailchimp_country>{MAILCHIMP_COMPANY_COUNTRY}</mailchimp_country>
 <mailchimp_zip>{MAILCHIMP_COMPANY_ZIP}</mailchimp_zip>
 <mailchimp_permission_reminder>{MAILCHIMP_TEXT_PERMISSION_REMINDER}</mailchimp_permission_reminder>
 <mailchimp_from_name>{MAILCHIMP_FROM_NAME}</mailchimp_from_name>
 <mailchimp_language>{MAILCHIMP_LANGUAGE}</mailchimp_language>
 <mailchimp_campaign_type>{MAILCHIMP_CAMPAIGN_TYPE}</mailchimp_campaign_type>
 <gateway_id>{GATEWAY_ID}</gateway_id>
 </mailet>
```