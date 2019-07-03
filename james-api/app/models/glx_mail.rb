class GlxMail < ApplicationRecord
  self.table_name = "glx_mails"
  self.primary_key = "id"

  # set current action
  attr_accessor :current_action

  # set time stamp
  attr_accessor :log_timestamp

  # relations
  belongs_to :status, class_name: 'GlxStatus', foreign_key: 'status_id'
  has_many :logs, :foreign_key => "mail_id", :class_name => "GlxStatusLog"
  has_one :sendgrid_mail_info, :foreign_key => "glx_mail_id", :class_name => "GlxSendgridMailInfo"
  has_one :aws_ses_mail_info, :foreign_key => "glx_mail_id", :class_name => "GlxAwsSesMailInfo"

  before_save :save_log

  def save_log
    if status_id_changed?
      old_value = status_id_change[0]
      new_value = status_id_change[1]
      log = GlxStatusLog.create(
        status_from_id: old_value,
        status_to_id: new_value,
        mail_id: id,
        action: current_action
      )
      if log_timestamp
        log.update_column(:created_at, DateTime.strptime(log_timestamp.to_s, '%s'))
      end
    end
  end
end
