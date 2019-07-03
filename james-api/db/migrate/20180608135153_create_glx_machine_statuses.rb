class CreateGlxMachineStatuses < ActiveRecord::Migration[5.1]
  def change
    create_table :glx_machine_statuses do |t|
      t.integer :initial_status_id
      t.string :action
      t.integer :final_status_id

      t.timestamps
    end
  end
end
