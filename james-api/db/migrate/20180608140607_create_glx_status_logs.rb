class CreateGlxStatusLogs < ActiveRecord::Migration[5.1]
  def change
    create_table :glx_status_logs do |t|
      t.string :mail_id
      t.integer :status_from_id
      t.string :action
      t.integer :status_to_id

      t.timestamps
    end
  end
end
