function readMessage(data) {
  const [_, type, id, payload] = data.match(/(\w+)(?:\[(\w+)\])?({.*})?/s)
  return [type, payload && JSON.parse(payload), id]
}

export class Socket {
  constructor(path) {
    this.ws = new WebSocket(`wss:${window.location.host}${path}${window.location.search}`)
    this.handlers = new Map()
    this.ws.onmessage = (msg) => {
      const [type, message, id] = readMessage(msg.data)
      const handler = this.handlers.get(type)
      if (!handler) {
        console.error(`No handler for type ${type}`)
        return
      }
      const respond = id ? (msg => this.send(`[${id}]`, msg)) : (msg => this.send("", msg))
      handler(message, respond)
    }
    this.on("Id", ({ id }) => sessionStorage.setItem("id", id))
    this.on("GetId", (_, respond) => respond({ id: sessionStorage.getItem("id") }))
    this.on("Error", (error) => console.error(error.message))
  }

  send(messageType, content) {
    const frame = content ? messageType + JSON.stringify(content) : messageType
    this.ws.send(frame)
  }

  on(type, handler) {
    this.handlers.set(type, handler)
  }
}
