class CreateGlxSendgridMailInfos < ActiveRecord::Migration[5.1]
  def change
    create_table :glx_sendgrid_mail_infos do |t|
      t.string :sendgrid_id
      t.integer :glx_mail_id
      t.timestamps
    end
  end
end
