class RenameTableGlxAwsSesMailInfo < ActiveRecord::Migration[5.1]
  def change
    rename_table :glx_aws_ses_mail_infos, :glx_aws_ses_mail_info
  end
end
