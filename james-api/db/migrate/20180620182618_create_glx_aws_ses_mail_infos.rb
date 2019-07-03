class CreateGlxAwsSesMailInfos < ActiveRecord::Migration[5.1]
  def change
    create_table :glx_aws_ses_mail_infos do |t|
      t.string :ses_id
      t.integer :glx_mail_id

      t.timestamps
    end
  end
end
