class RenameTableGlxSendgridMailInfo < ActiveRecord::Migration[5.1]
  def change
    rename_table :glx_sendgrid_mail_infos, :glx_sendgrid_mail_info
  end
end
