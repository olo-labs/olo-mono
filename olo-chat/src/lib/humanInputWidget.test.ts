/**
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */

import { describe, expect, it } from 'vitest'
import type { HumanStepParameter } from './chatHumanStep'
import {
  approvalTogglesAllowApprove,
  defaultFieldValue,
  isParameterValueValid,
  resolveHumanInputWidget,
} from './humanInputWidget'

function param(overrides: Partial<HumanStepParameter> = {}): HumanStepParameter {
  return { id: 'field', ...overrides }
}

describe('resolveHumanInputWidget', () => {
  it('prefers ui.widget over type', () => {
    expect(
      resolveHumanInputWidget(
        param({ type: 'string', ui: { widget: 'APPROVAL_TOGGLE' } })
      )
    ).toBe('APPROVAL_TOGGLE')
  })

  it('maps textarea and boolean types', () => {
    expect(resolveHumanInputWidget(param({ type: 'textarea' }))).toBe('TEXTAREA')
    expect(resolveHumanInputWidget(param({ type: 'boolean' }))).toBe('BOOLEAN')
  })
})

describe('approval toggle validation', () => {
  it('requires explicit yes/no selection when required', () => {
    const toggle = param({
      id: 'approveRestart',
      required: true,
      ui: { widget: 'APPROVAL_TOGGLE' },
      type: 'boolean',
    })
    expect(isParameterValueValid(toggle, '')).toBe(false)
    expect(isParameterValueValid(toggle, 'true')).toBe(true)
    expect(approvalTogglesAllowApprove([toggle], { approveRestart: 'false' })).toBe(false)
    expect(approvalTogglesAllowApprove([toggle], { approveRestart: 'true' })).toBe(true)
  })

  it('defaults approval toggle to unselected', () => {
    expect(defaultFieldValue(param({ ui: { widget: 'APPROVAL_TOGGLE' }, type: 'boolean' }))).toBe('')
  })
})
