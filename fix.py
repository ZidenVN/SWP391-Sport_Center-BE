import os

def fix_file(filename, sql_del, sql_res):
    with open(filename, 'r') as f:
        content = f.read()
    
    # Simple replace
    lines = content.split('\n')
    new_lines = []
    for line in lines:
        if line.startswith('@SQLDelete'):
            new_lines.append(sql_del)
        elif line.startswith('@SQLRestriction'):
            new_lines.append(sql_res)
        else:
            new_lines.append(line)
            
    with open(filename, 'w') as f:
        f.write('\n'.join(new_lines))

fix_file('src/main/java/com/fptu/swp391/sportscentermanager/entity/Room.java', '@SQLDelete(sql = "update rooms set status = \'INACTIVE\' where room_id = ?")', '@SQLRestriction("status = \'ACTIVE\'")')
fix_file('src/main/java/com/fptu/swp391/sportscentermanager/entity/SportClass.java', '@SQLDelete(sql = "update classes set status = \'INACTIVE\' where class_id = ?")', '@SQLRestriction("status = \'OPENING\'")')
