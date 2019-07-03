class CreateGlxStatuses < ActiveRecord::Migration[5.1]
  def change
    create_table :glx_statuses do |t|
      t.string :name

      t.timestamps
    end
  end
end
