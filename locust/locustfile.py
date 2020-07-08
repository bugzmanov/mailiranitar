from locust import task, SequentialTaskSet, HttpUser, TaskSet, between
import random


global_box_list = []  # need to keep global list of users to get some concurrent access
global_message_list = {}

def post_message_ind(task_set):
    if not task_set.mailbox in global_message_list:
        return

    resp = task_set.client.post("/mailboxes/"+task_set.mailbox+"/messages", json = {
        'sender': 'ololo@gmail.com',
        'subject': 'we need to talk about ololo2',
        'body': 'Call me ASAP',
        'sent':'2020-07-07T13:20:14.714Z'
        }, name = "post message")

    try:
        message_id = resp.json()['id']
        if task_set.mailbox in global_message_list:
            global_message_list[task_set.mailbox].append(message_id)
    except:
        pass




class SequenceOfTasks(SequentialTaskSet):

    @task(1)
    def create_mailbox(self):
        if random.random() < 0.3 or len(global_box_list) == 0:
            resp = self.client.post("/mailboxes", {})
            self.mailbox = resp.json()['email']
            global_box_list.append(self.mailbox)
            global_message_list[self.mailbox] = []
        else:
            self.mailbox = random.choice(global_box_list)

    tasks = [post_message_ind,post_message_ind,
             post_message_ind,post_message_ind,
             post_message_ind,post_message_ind]

    @task
    def read_from_page(self):
        if not self.mailbox in global_message_list:
            return

        messages = global_message_list[self.mailbox]
        if not messages:
            return

        data = self.client.get("/mailboxes/" + self.mailbox + "/messages?from=" + random.choice(messages),
                       name = "read messages by page")

    @task
    def read_messages(self):
        if not self.mailbox in global_message_list:
            return
        messages = global_message_list[self.mailbox]
        if not messages:
            return

        mid = random.choice(messages)
        # self.messages.remove(mid)
        self.client.get("/mailboxes/" + self.mailbox + "/messages/" + mid, name = "read message")

    @task
    def delete_messages(self):
        if not self.mailbox in global_message_list:
            return
        # for _ in range(2):

        messages = global_message_list[self.mailbox]
        if not messages:
            return

        mid = random.choice(messages)
        global_message_list[self.mailbox].remove(mid)
        self.client.delete("/mailboxes/" + self.mailbox + "/messages/" + mid, name = "delete message")

    @task
    def read_message(self):
        pass

    @task
    def maybe_remove_mailbox(self):
        if not self.mailbox in global_message_list:
            return

        if random.random() < 0.1:
            pass
            global_box_list.remove(self.mailbox)
            global_message_list.pop(self.mailbox, None)
            self.client.delete("/mailboxes/" + self.mailbox, name = "remove mailbox")
        pass

class WebsiteUser(HttpUser):
    tasks = {SequenceOfTasks: 100}
    wait_time = between(0.0, 2.0)

