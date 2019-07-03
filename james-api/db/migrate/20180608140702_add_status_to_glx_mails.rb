class AddStatusToGlxMails < ActiveRecord::Migration[5.1]
  def change
    add_column :glx_mails, :status_id, :integer
  end
end
