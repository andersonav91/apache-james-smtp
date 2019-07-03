package org.glx.mailets;

import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;
import org.apache.mailet.base.GenericMailet;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collection;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimePart;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.util.regex.Pattern;
import org.glx.utils.DatabaseConnection;
import org.apache.commons.codec.binary.Base64;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONArray;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import java.util.Date;

public class GLXMailchimpRelay extends GenericMailet {
    private static final String HTML_BR_TAG = "<br />";
    private String mailChimpEmailFrom = "";
    private String mailChimpApiKey = "";
    private String mailChimpDataCenter = "";
    private Connection connection;
    private int gatewayId = 0;
    private String mailChimpUrl = "";
    private String company = "";
    private String address1 = "";
    private String city = "";
    private String state = "";
    private String zip = "";
    private String country = "";
    private String permissionReminder = "";
    private String fromName = "";
    private String language = "";
    private String campaignType = "";

    @Override
    public void init() throws MessagingException {
        mailChimpEmailFrom = getInitParameter("mailchimp_email_from");
        mailChimpDataCenter = getInitParameter("mailchimp_data_center");
        gatewayId = Integer.parseInt(getInitParameter("gateway_id"));
        mailChimpApiKey = getInitParameter("mailchimp_api_key");
        mailChimpUrl = "https://" + mailChimpDataCenter + ".api.mailchimp.com/3.0/";
        company = getInitParameter("mailchimp_company");
        address1 = getInitParameter("mailchimp_address1");
        city = getInitParameter("mailchimp_city");
        state = getInitParameter("mailchimp_state");
        zip = getInitParameter("mailchimp_zip");
        country = getInitParameter("mailchimp_country");
        permissionReminder = getInitParameter("mailchimp_permission_reminder");
        fromName = getInitParameter("mailchimp_from_name");
        language = getInitParameter("mailchimp_language");
        campaignType = getInitParameter("mailchimp_campaign_type");
    }

    @Override
    public String getMailetInfo() {
        return "GLXMailchimpRelay Mailet";
    }

    @Override
    public void service(Mail mail) throws MessagingException {
        this.connection = DatabaseConnection.getConnection();
        String authString = "jamesuser:".concat(mailChimpApiKey);
        byte[] authEncBytes = Base64.encodeBase64(authString.getBytes());
        String authStringEnc = new String(authEncBytes);
        // Each all recipients for the email
        Collection<MailAddress> recipients = mail.getRecipients();
        for (MailAddress mailAddress : recipients) {
            MimeMessage mimeMessage = mail.getMessage();
            String emailContent = "";
            String msgType = "";
            try{
                emailContent = getContent(mimeMessage);
                if(isHtml(emailContent)){
                    msgType = "html";
                }
                else{
                    msgType = "text";
                }
            }
            catch(IOException ex){
                log("GLXMailchimpRelay: Error getting the content message " + ex.getMessage());
            }
            // create the list
            JSONObject listParams = paramsForList(mailAddress.toString(), mail.getMessage().getSubject());
            HttpClient client = HttpClientBuilder.create().build();
            HttpPost postCreateList = new HttpPost(mailChimpUrl + "lists");
            JsonNode responseCreateList = null;
            try {
                postCreateList.setEntity(new StringEntity(listParams.toString(), "UTF8"));
                postCreateList.setHeader("Content-type", "application/json");
                postCreateList.setHeader("Authorization", "Basic " + authStringEnc);
                postCreateList.setHeader("Accept", "application/json");
                HttpResponse resp = client.execute(postCreateList);
                String jsonString = EntityUtils.toString(resp.getEntity());
                ObjectMapper mapper = new ObjectMapper();
                responseCreateList = mapper.readTree(jsonString);
            } catch (Exception ex) {
                log("GLXMailchimpRelay: invalid request (post/lists) " + ex.getMessage());
            }

            String mailChimpListId = responseCreateList.get("id").textValue();

            // subscribe member to the list
            JSONObject listMemberParams = paramsForListMembers(mailAddress.toString(), msgType);
            HttpPost postListMembers = new HttpPost(mailChimpUrl + "lists/" + mailChimpListId);
            JsonNode responseListMembers = null;
            try {
                postListMembers.setEntity(new StringEntity(listMemberParams.toString(), "UTF8"));
                postListMembers.setHeader("Content-type", "application/json");
                postListMembers.setHeader("Authorization", "Basic " + authStringEnc);
                postListMembers.setHeader("Accept", "application/json");
                HttpResponse resp = client.execute(postListMembers);
                String jsonString = EntityUtils.toString(resp.getEntity());
                ObjectMapper mapper = new ObjectMapper();
                responseListMembers = mapper.readTree(jsonString);
            } catch (Exception ex) {
                log("GLXMailchimpRelay: invalid request (post/lists/addmember) " + ex.getMessage());
            }

            // create template
            JSONObject createTemplateParams = paramsForTemplate(mailAddress.toString(), emailContent);
            HttpPost postCreateTemplate = new HttpPost(mailChimpUrl + "templates");
            JsonNode responseCreateTemplate = null;
            try {
                postCreateTemplate.setEntity(new StringEntity(createTemplateParams.toString(), "UTF8"));
                postCreateTemplate.setHeader("Content-type", "application/json");
                postCreateTemplate.setHeader("Authorization", "Basic " + authStringEnc);
                postCreateTemplate.setHeader("Accept", "application/json");
                HttpResponse resp = client.execute(postCreateTemplate);
                String jsonString = EntityUtils.toString(resp.getEntity());
                ObjectMapper mapper = new ObjectMapper();
                responseCreateTemplate = mapper.readTree(jsonString);
            } catch (Exception ex) {
                log("GLXMailchimpRelay: invalid request (post/templates) " + ex.getMessage());
            }

            String mailChimpTemplateId = responseCreateTemplate.get("id").asText();
            // create campaing
            JSONObject createCampaignParams = paramsForCampaign(mailChimpListId, mail.getMessage().getSubject() , mailChimpTemplateId);
            HttpPost postCreateCampaign = new HttpPost(mailChimpUrl + "campaigns");
            JsonNode responseCreateCampaign = null;
            try {
                postCreateCampaign.setEntity(new StringEntity(createCampaignParams.toString(), "UTF8"));
                postCreateCampaign.setHeader("Content-type", "application/json");
                postCreateCampaign.setHeader("Authorization", "Basic " + authStringEnc);
                postCreateCampaign.setHeader("Accept", "application/json");
                HttpResponse resp = client.execute(postCreateCampaign);
                String jsonString = EntityUtils.toString(resp.getEntity());
                ObjectMapper mapper = new ObjectMapper();
                responseCreateCampaign = mapper.readTree(jsonString);
            } catch (Exception ex) {
                log("GLXMailchimpRelay: invalid request (post/campaigns) " + ex.getMessage());
            }

            String mailChimpCampaignId = responseCreateCampaign.get("id").asText();
            // send campaing
            HttpPost postSendCampaign = new HttpPost(mailChimpUrl + "campaigns/" + mailChimpCampaignId + "/actions/send");
            JsonNode responseSendCampaign = null;
            try {
                postSendCampaign.setHeader("Content-type", "application/json");
                postSendCampaign.setHeader("Authorization", "Basic " + authStringEnc);
                postSendCampaign.setHeader("Accept", "application/json");
                HttpResponse resp = client.execute(postSendCampaign);
                String jsonString = EntityUtils.toString(resp.getEntity());
                ObjectMapper mapper = new ObjectMapper();
                responseSendCampaign = mapper.readTree(jsonString);
            } catch (Exception ex) {
                log("GLXMailchimpRelay: invalid request (post/campaigns/send) " + ex.getMessage());
            }
            saveMail(mailAddress.toString(), mail.getName());
        }
        // Set mail as ghost mail
        mail.setState(Mail.GHOST);
    }


    private JSONObject paramsForListMembers(String to, String emailType) {
        JSONObject params = new JSONObject();
        JSONObject member = new JSONObject();
        member.put("email_address", to);
        member.put("email_type", emailType);
        member.put("status", "subscribed");
        member.put("language", language);
        JSONArray members = new JSONArray();
        members.appendElement(member);
        params.put("members", members);
        params.put("update_existing", false);
        return params;
    }

    private JSONObject paramsForList(String to, String subject) {
        Date today = Calendar.getInstance().getTime();
        JSONObject params = new JSONObject();
        String listName = to + "-" + DateFormatUtils.format(today, "yyyy-MM-dd-HH-mm") + "-list";
        params.put("name", listName);
        JSONObject contact = new JSONObject();
        contact.put("company", company);
        contact.put("address1", address1);
        contact.put("city", city);
        contact.put("state", state);
        contact.put("zip", zip);
        contact.put("country", country);
        params.put("contact", contact);
        params.put("permission_reminder", permissionReminder);
        JSONObject campaignDefaults = new JSONObject();
        campaignDefaults.put("from_name", fromName);
        campaignDefaults.put("from_email", mailChimpEmailFrom);
        campaignDefaults.put("subject", subject);
        campaignDefaults.put("language", language);
        params.put("campaign_defaults", campaignDefaults);
        params.put("email_type_option", true);
        return params;
    }

    private JSONObject paramsForTemplate(String to, String content) {
        Date today = Calendar.getInstance().getTime();
        JSONObject params = new JSONObject();
        String listName = to + "-" + DateFormatUtils.format(today, "yyyy-MM-dd-HH-mm") + "-template";
        params.put("name", listName);
        params.put("html", content);
        return params;
    }

    private JSONObject paramsForCampaign(String listId, String subject, String templateId) {
        JSONObject params = new JSONObject();
        params.put("type", campaignType);
        JSONObject recipients = new JSONObject();
        recipients.put("list_id", listId);
        params.put("recipients", recipients);
        JSONObject settings = new JSONObject();
        settings.put("subject_line", subject);
        settings.put("title", subject);
        settings.put("template_id", Integer.parseInt(templateId));
        settings.put("from_name", fromName);
        settings.put("repply_to", mailChimpEmailFrom);
        settings.put("from_email", mailChimpEmailFrom);
        params.put("settings", settings);
        return params;
    }

    public String getContent(MimePart part) throws MessagingException, IOException{
        String contentType = part.getContentType();
        if (part.getContent() instanceof String) {
            if (part.isMimeType("text/plain")) {
                String content = (String) part.getContent();
                if (! content.equals(null) && ! content.equals("")) {
                    return content;
                }
            } else if (part.isMimeType("text/html")) {
                String content = (String) part.getContent();
                if (! content.equals(null) && ! content.equals("")) {
                    String [] htmlParts = content.split("(?<=</html>)");
                    Document doc;
                    for(String bodyPart : htmlParts){
                        doc = Jsoup.parse(bodyPart);
                        return doc.body().html();
                    }
                    return "";
                }
                return "";
            }
            return "";
        }

        if (part.isMimeType("multipart/mixed")
                || part.isMimeType("multipart/related")) {
            log("GLXSendgridRelay: related");
            MimeMultipart multipart = (MimeMultipart) part.getContent();
            MimeBodyPart firstPart = (MimeBodyPart) multipart.getBodyPart(0);
            return getContent(firstPart);
        } else if (part.isMimeType("multipart/alternative")) {
            log("GLXSendgridRelay: multipart");
            MimeMultipart multipart = (MimeMultipart) part.getContent();
            int count = multipart.getCount();
            for (int index = 0; index < count; index++) {
                MimeBodyPart mimeBodyPart = (MimeBodyPart) multipart.getBodyPart(index);
                if(mimeBodyPart.isMimeType("text/html")){
                    return getContent(mimeBodyPart);
                }
            }
            return "";
        }
        return "";
    }

    public void saveMail(String toEmail, String mailName){
        Calendar calendar = Calendar.getInstance();
        Timestamp sentAt = new Timestamp(calendar.getTime().getTime());

        String query = "INSERT INTO glx_mails (gateway, mail_name, to_email, sent_at, from_email) ";
        query = query.concat("VALUES (?, ?, ?, ?, ?);");

        PreparedStatement preparedStatement = null;
        try{
            preparedStatement = this.connection.prepareStatement(query);
        }
        catch(SQLException sqlException){
            log("GLXSendgridRelay: error creating the insert for the email " + toEmail + " on the gateway " + Integer.toString(gatewayId));
        }
        try{
            preparedStatement.setInt(1, gatewayId);
            preparedStatement.setString(2, mailName);
            preparedStatement.setString(3, toEmail);
            preparedStatement.setTimestamp(4, sentAt);
            preparedStatement.setString(5, mailChimpEmailFrom);
            preparedStatement.executeUpdate();
        }
        catch(SQLException sqlException){
            log("GLXSendgridRelay: error inserting the email " + toEmail + " on the gateway " + Integer.toString(gatewayId));
        }
    }

    public boolean isHtml(String content){
        Pattern htmlPattern = Pattern.compile(".*\\<[^>]+>.*", Pattern.DOTALL);
        return htmlPattern.matcher(content).matches();
    }
}
