class GlxStatusLog < ApplicationRecord
  belongs_to :mail, :foreign_key => "mail_id", :class_name => "GlxMail"
  belongs_to :status_from, :foreign_key => "status_from_id", :class_name => "GlxStatus"
  belongs_to :status_to, :foreign_key => "status_to_id", :class_name => "GlxStatus"
end
