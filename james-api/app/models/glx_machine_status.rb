class GlxMachineStatus < ApplicationRecord
  belongs_to :initial_status, :foreign_key => "initial_status_id", :class_name => "GlxStatus"
  belongs_to :final_status, :foreign_key => "final_status_id", :class_name => "GlxStatus"

  def self.get_status_from_initial_status_and_action(initial_status_id, action)
    final_status_id = 0
    status = where(initial_status_id: initial_status_id)
    .where('lower(action) = ?', action.downcase).first
    if ! status.nil?
      final_status_id = status.final_status_id
    else
      status = where(initial_status_id: GlxStatus::STATUS_ALL_ID)
          .where('lower(action) = ?', action.downcase).first
      if ! status.nil?
        final_status_id = status.final_status_id
      end
    end

    return final_status_id
  end
end
