class GlxAwsSesMailInfo < ApplicationRecord

  # table name
  self.table_name = "glx_aws_ses_mail_info"

  #relations
  belongs_to :mail, class_name: 'GlxMail', foreign_key: 'glx_mail_id'

  def self.get_mail_from_aws_ses_id(ses_id)
    find_by_ses_id(ses_id).mail rescue GlxMail.new
  end
end
