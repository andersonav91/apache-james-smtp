class GlxSendgridMailInfo < ApplicationRecord

  # table name
  self.table_name = "glx_sendgrid_mail_info"

  #relations
  belongs_to :mail, class_name: 'GlxMail', foreign_key: 'glx_mail_id'

  def self.get_mail_from_sendgrid_id(sendgrid_id)
    find_by_sendgrid_id(sendgrid_id).mail rescue GlxMail.new
  end
end
