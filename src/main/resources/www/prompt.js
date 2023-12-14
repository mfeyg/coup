import { html } from "htm/preact"
import { useState } from "preact/hooks"

function ActionPrompt({ options, onSelect }) {
  return html`
    Choose an action:
    <ul class="buttons">
    ${options.map(option => html`
      <li><button onclick=${() => onSelect(option)}>${option.actionType}</button></li>
    `)}
    </ul>
  `
}

function TargetPrompt({ targets, onSelect }) {
  return html`
    Choose a target:
    <ul class="buttons">
    ${targets.map(target => html`
      <li><button onclick=${() => onSelect(target)}>${target.name}</button></li>
    `)}
    </ul>
    `
}

function TakeTurnPrompt({ options, onSelect }) {
  const [action, setAction] = useState(null)

  function selectAction(option) {
    if (!option.targets.length) {
      onSelect({ actionType: option.actionType })
    } else {
      setAction(option)
    }
  }

  function selectTarget(target) {
    onSelect({ actionType: action.actionType, target: target.number })
  }

  if (action == null) {
    return html`<${ActionPrompt} options=${options} onSelect=${selectAction} />`
  } else {
    return html`<${TargetPrompt} targets=${action.targets} onSelect=${selectTarget} />`
  }
}

function ResponsePrompt({ blockingInfluences, claimedInfluence, onSelect }) {
  const [blocking, setBlocking] = useState(false)
  if (blocking) {
    return html`
      <p>Choose an influence to block as:</p>
      ${blockingInfluences.map(influence => html`
        <button onclick=${() => onSelect({ reaction: "Block", influence })}>${influence}</button>`)}
    `
  }
  function AllowButton() {
    return html`<button onclick=${() => onSelect({ reaction: "Allow" })}>Allow</button>`
  }
  function BlockButton() {
    return blockingInfluences && html`<button onclick=${() => setBlocking(true)}>Block</button>`
  }
  function ChallengeButon() {
    return claimedInfluence && html`<button onclick=${() => onSelect({ reaction: "Challenge" })}>Challenge claim of ${claimedInfluence}</button>`
  }
  return html`
    <p>How will you respond?</p>
    <${AllowButton} />
    <${BlockButton} />
    <${ChallengeButon} />
  `
}

function PromptReturn({ influences, number, onSelect }) {
  const [selected, setSelected] = useState([])
  function select(influence) {
    if (selected.length + 1 === number) {
      onSelect(selected.concat(influence))
    } else {
      setSelected(selected.concat(influence))
    }
  }
  selected.forEach(influence => influences.splice(influences.indexOf(influence), 1))
  return html`
  <p>Select influences to return:</p>
  ${influences.map(influence => html`
    <button onclick=${() => select(influence)}>${influence}</button>`)}
  `
}

function actionDescription(actor, actionType, target) {
  switch (actionType) {
    case "Foreign Aid":
      return `${actor} wishes to receive foreign aid.`
    case "Tax":
      return `${actor} wishes to collect taxes.`
    case "Steal":
      return `${actor} wishes to steal from ${target}.`
    case "Exchange":
      return `${actor} wishes to exchange with the deck.`
    case "Assassinate":
      return `${actor} wishes to assassinate ${target}.`
  }
}

export function Prompt({ prompt, player, opponents }) {
  const { type, message } = prompt ?? {}
  const respond = (response) => {
    prompt.respond(response)
  }
  let dialog

  switch (type) {
    case "TakeTurn":
      dialog = html`<${TakeTurnPrompt} options=${message.options} onSelect=${respond} />`
      break;
    case "RespondToAction":
      dialog = html`
      <p>${actionDescription(
        opponents[message.player].name,
        message.type,
        message.target === player.number ? "you" : opponents[message.target]?.name
      )}</p>
      <${ResponsePrompt}
        blockingInfluences=${message.canBeBlocked && message.blockingInfluences}
        claimedInfluence=${message.canBeChallenged && message.claimedInfluence}
        onSelect=${respond} />
      `
      break;
    case "RespondToBlock":
      dialog = html`
      <p>${opponents[message.blocker].name} wishes to block as the ${message.blockingInfluence}.</p>
      <${ResponsePrompt}
        claimedInfluence=${message.blockingInfluence}
        onSelect=${respond}/>
      `
      break;
    case "RespondToChallenge":
      dialog = html`
      <p>${opponents[message.challenger].name} challenges your claim of ${message.claim}.</p>
      <p>While influence will you show?</p>
      ${player.heldInfluences.map(influence => html`<button onclick=${() => respond({ influence })}>${influence}</button>`)}
      `
      break;
    case "SurrenderInfluence":
      dialog = html`
      <p>Select an influence to surrender:</p>
      ${player.heldInfluences.map(influence => html`<button onclick=${() => respond({ influence })}>${influence}</button>`)}
      `
      break;
    case "Exchange":
      dialog = html`
      <p>Your influences: ${player.heldInfluences.join(", ")}</p>
      <p>You drew: ${message.drawnInfluences.join(", ")}</p>
      <${PromptReturn}
        influences=${player.heldInfluences.concat(message.drawnInfluences)}
        number=${2} 
        onSelect=${influences => respond({ returnedInfluences: influences })} />
      `
      break;
  }

  return html`
    <dialog open=${!!prompt}>
    ${dialog}
    </dialog>
  `
}