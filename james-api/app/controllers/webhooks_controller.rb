require 'net/http'
class WebhooksController < ApplicationController
  def sendgrid
    params["_json"].each do |obj|
      mail_name = obj["mail_name"] rescue nil
      next unless mail_name # return if mail is test mail
      sendgrid_id = obj["sg_message_id"]
      if mail_name
        email = GlxMail.find_by_mail_name(mail_name)
        return if email.nil?
        unless GlxSendgridMailInfo.exists?(glx_mail_id: email.id)
          GlxSendgridMailInfo.create(glx_mail_id: email.id, sendgrid_id: sendgrid_id)
        end
      else
        email = GlxSendgridMailInfo.get_mail_from_sendgrid_id(sendgrid_id)
      end
      # set the final status
      action = obj["event"]
      action = action.capitalize
      email.current_action = action
      email.log_timestamp = obj["timestamp"]
      final_status_id = GlxMachineStatus.get_status_from_initial_status_and_action(email.status_id, action) rescue 0
      if final_status_id != 0
        email.update_attribute(:status_id, final_status_id)
      else
        Rails.logger.debug("Error: #{mail_name} #{action} #{email.status_id}")
      end
    end

    render nothing: true, status: :ok, content_type: "text/html"
  end

  def ses
    @data ||= JSON.parse request.raw_post

    # confirm the endpoint
    if @data["SubscribeURL"]
      url = URI.parse(@data["SubscribeURL"])
      req = Net::HTTP::Get.new(url.to_s)
      res = res = Net::HTTP.start(url.host, url.port,
                                  :use_ssl => url.scheme == 'https') {|http| http.request req}
    end
    info = @data["Message"] rescue nil
    if info
      info = JSON.parse(info)
      action = info["notificationType"]
      mail_id = info["mail"]['messageId'] rescue nil
      return unless mail_id # return if mail is test mail

      email = GlxAwsSesMailInfo.get_mail_from_aws_ses_id(mail_id)
      email.current_action = action
      email.log_timestamp = info["mail"]["timestamp"].to_datetime.to_i rescue Time.now.getutc
      final_status_id = GlxMachineStatus.get_status_from_initial_status_and_action(email.status_id, action) rescue 0
      if final_status_id != 0
        email.update_attribute(:status_id, final_status_id)
      else
        Rails.logger.debug("Error: #{mail_name} #{action} #{email.status_id}")
      end
    end
    render json: @data, status: :ok, content_type: "text/html"
  end

  def test
    render json: { status: "ok" }, status: :ok, content_type: "text/html"
  end
end
